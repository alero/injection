/*
 * Copyright (c) 2017 org.hrodberaht
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hrodberaht.injection.plugin.junit.solr;

import org.apache.commons.io.FileUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.hrodberaht.injection.plugin.exception.PluginRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hrodberaht.injection.plugin.junit.plugins.SolrJPlugin.DEFAULT_HOME;
import static org.hrodberaht.injection.plugin.junit.plugins.SolrJPlugin.DEFAULT_RUNNER_HOME;

public class SolrTestRunner {

    private static final Logger LOG = LoggerFactory.getLogger(SolrTestRunner.class);
    private static final String classpathPrefix = "classpath:";
    private static Map<String, SolrRunnerHolder> CORE_CACHE = new ConcurrentHashMap<>();
    private String base;
    private String home;
    private String defaultCoreName;
    private List<String> coreNames;

    private static void copyFile(final String copyToDir, final String resourceStream, final String fileName) throws IOException {
        LOG.info("copyFile from:{} toDir:{} toFile:{}", resourceStream, copyToDir, fileName);
        final InputStream inputStream = SolrTestRunner.class.getClassLoader().getResourceAsStream(resourceStream);
        new File(copyToDir).mkdirs();
        Files.copy(inputStream, new File(copyToDir, fileName).toPath(), REPLACE_EXISTING);
    }

    public void setup() {
        setup(DEFAULT_RUNNER_HOME);
    }

    public void setup(String solrHome) {
        setup(DEFAULT_HOME, solrHome, "collection1");
    }

    public void setup(String solrHome, String solrRunnerHome, String defaultCore, String ...cores) {
        coreNames = new ArrayList<>();
        try {
            this.base = solrHome;
            this.home = solrRunnerHome;
            this.defaultCoreName = defaultCore;
            this.coreNames.add(defaultCore);
            if (cores != null) {
                coreNames.addAll(Arrays.asList(cores));
            }
            LOG.info("setup {} - {} - {} - {}", base, home, defaultCoreName, coreNames.stream().reduce("", String::concat));
            setupSolr();
        } catch (IOException e) {
            throw new PluginRuntimeException(e);
        }
    }

    public SolrAssertions solrAssertions() {
        return new SolrAssertions(getServer());
    }

    public SolrAssertions solrAssertions(String coreName) {
        return new SolrAssertions(getServer(), coreName, null, null);
    }

    public SolrClient getClient() {
        return getServer();
    }

    private void setupSolr() throws IOException {
        perpareSolrHomeAndStart();
    }

    private void perpareSolrHomeAndStart() throws IOException {
        String runnerName = runnerName();
        CORE_CACHE.computeIfAbsent(runnerName, runnerNameInner -> {
            try {
                LOG.info(" ----------- SolrTestRunner setup --- STARTING! name{} performing tearDown, copyFiles and createContainer ", runnerName);
                tearDown();
                moveConfigFiles();
                LOG.info("Loading Solr container {}", runnerName);
                SolrRunnerHolder solrRunnerHolder = createSolrContainer(runnerNameInner);
                LOG.info("Loading Solr DONE container {}", runnerName);
                return solrRunnerHolder;
            } catch (IOException e) {
                LOG.error("Bad container", e);
                throw new PluginRuntimeException(e);
            }
        });

    }

    private EmbeddedSolrServer getServer() {
        String runnerName = runnerName();
        SolrRunnerHolder solrRunnerHolder = CORE_CACHE.get(runnerName);
        LOG.debug("getServer with name {}", runnerName);
        return solrRunnerHolder.solr;
    }

    private SolrRunnerHolder createSolrContainer(String runnerName) {
        // TODO: figure out if its possible to create a reusable core/collection service cache
        CoreContainer coreContainer = SolrVersionManagement.createContainer(home);
        coreContainer.load();
        LOG.info("Loading embedded container {}", runnerName);
        EmbeddedSolrServer solr = new EmbeddedSolrServer(coreContainer, defaultCoreName);
        return new SolrRunnerHolder(coreContainer, solr);
    }

    public void shutdownServer() {
        String runnerName = runnerName();
        SolrRunnerHolder solrRunnerHolder = CORE_CACHE.get(runnerName);
        LOG.info("shutdownServer of SolrRunnerHolder with name {}", runnerName);
        try {
            solrRunnerHolder.solr.close();
        } catch (IOException e) {
            throw new PluginRuntimeException(e);
        }
        solrRunnerHolder.coreContainer.shutdown();
    }

    private String runnerName() {
        return home + "/" + defaultCoreName;
    }

    public void cleanSolrInstance() {
        try {
            EmbeddedSolrServer solr = getServer();
            solr.deleteByQuery("*:*");
            solr.commit(true, true, false);
        } catch (SolrServerException | IOException e) {
            throw new PluginRuntimeException(e);
        }
    }

    private void moveConfigFiles() throws IOException {
        moveSolrConfigFile(home, "solr/solr.xml", "solr.xml");
        for (String coreName : this.coreNames) {
            moveFiles(home, coreName);
            moveFiles(home, coreName + "/conf");

            try {
                moveFiles(home, coreName + "/conf/lang");
            } catch (UnsupportedOperationException e) {
                LOG.warn("found no files to copy at : '" + coreName + "/conf/lang'");
            }
        }
    }

    private void moveSolrConfigFile(String solrHome, String resourceStream, String fileName) throws IOException {
        copyFile(solrHome, resourceStream, fileName);
    }

    private void moveFiles(String solrHome, String path) throws IOException {
        if (base.startsWith(classpathPrefix)) {
            String solrBase = base.substring(classpathPrefix.length(), base.length());
            for (String fileName : getResourceListing(SolrTestRunner.class, solrBase + "/" + path)) {
                copyFile(solrHome + "/" + path, solrBase + "/" + path + "/" + fileName, fileName);
            }
        } else {
            throw new PluginRuntimeException("Only classpath resources supported at the moment");
        }
    }

    private List<String> getResourceListing(Class<?> clazz, String path) throws IOException {
        LOG.info("getResourceListing using path: {}", path);
        List<String> foundFiles = new ArrayList<>();
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (findFilesFromFilessystem(dirURL, foundFiles)) {
            LOG.debug("Found files on filesystem : {}", foundFiles.size());
            return foundFiles;
        }

        if (dirURL == null) { // its probably in a jar file
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }
        if (dirURL == null) {
            throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
        }

        if ("jar".equals(dirURL.getProtocol())) {
            /* A JAR path */
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf('!')); //strip out only the JAR file

            if (findJarFilesInPath(path, foundFiles, jarPath)) {
                LOG.debug("Found files in jar-resource : {}", foundFiles.size());
                return foundFiles;
            }
        }

        throw new UnsupportedOperationException("Found NO files using filesystem or jar parts for URL " + dirURL);

    }

    private boolean findFilesFromFilessystem(URL dirURL, List<String> foundFiles) throws IOException {
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
            try {
                Stream.of(new File(dirURL.toURI()).listFiles()).forEach(file -> {
                    if (!file.isDirectory()) {
                        foundFiles.add(file.getName());
                    }
                });
                return true;
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        return false;
    }

    private boolean findJarFilesInPath(String path, List<String> foundFiles, String jarPath) throws IOException {
        try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"))) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith(path) && !jarEntry.isDirectory()) {
                    String entry = name.substring(path.length() + 1);
                    if (!entry.contains("/")) { // only add files on the same level
                        foundFiles.add(entry);

                    }
                }
            }
            return !foundFiles.isEmpty();
        }
    }

    private void tearDown() throws IOException {
        for (String coreName : this.coreNames) {
            File coreDir = new File(home, coreName);
            if (coreDir.exists()) {
                LOG.info("cleaning : " + coreDir.getAbsolutePath());
                FileUtils.cleanDirectory(coreDir);
            }
        }
    }

    private class SolrRunnerHolder {
        private final CoreContainer coreContainer;
        private final EmbeddedSolrServer solr;

        public SolrRunnerHolder(CoreContainer coreContainer, EmbeddedSolrServer solr) {
            this.coreContainer = coreContainer;
            this.solr = solr;
        }
    }


}
