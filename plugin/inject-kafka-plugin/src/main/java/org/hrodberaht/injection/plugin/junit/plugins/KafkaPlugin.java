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

package org.hrodberaht.injection.plugin.junit.plugins;


import com.salesforce.kafka.test.KafkaProvider;
import com.salesforce.kafka.test.KafkaTestServer;
import org.apache.commons.io.FileUtils;
import org.hrodberaht.injection.plugin.exception.PluginRuntimeException;
import org.hrodberaht.injection.plugin.junit.api.Plugin;
import org.hrodberaht.injection.plugin.junit.api.PluginContext;
import org.hrodberaht.injection.plugin.junit.api.annotation.RunnerPluginAfterClassTest;
import org.hrodberaht.injection.plugin.junit.api.annotation.RunnerPluginAfterTest;
import org.hrodberaht.injection.plugin.junit.api.annotation.RunnerPluginBeforeClassTest;
import org.hrodberaht.injection.plugin.junit.api.annotation.RunnerPluginBeforeContainerCreation;
import org.hrodberaht.injection.plugin.junit.api.annotation.RunnerPluginBeforeTest;
import org.hrodberaht.injection.plugin.junit.api.resource.ResourceProvider;
import org.hrodberaht.injection.plugin.junit.api.resource.ResourceProviderSupport;
import org.hrodberaht.injection.plugin.junit.plugins.common.PluginLifeCycledResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class KafkaPlugin implements Plugin, ResourceProviderSupport {

    public static final String CONNECTION_STRING = "kafka-broker-connection";
    private static final Logger LOG = LoggerFactory.getLogger(KafkaPlugin.class);

    private static final String LOG_DIR = "log.dir";
    private PluginLifeCycledResource<KafkaEmbeddedServer> pluginLifeCycledResource = new PluginLifeCycledResource<>(KafkaEmbeddedServer.class);

    private KafkaEmbeddedServer embeddedKafka;

    private String name = null;
    private boolean deleteTempDir = true;
    private Properties kafkaConfig = null;
    private LifeCycle lifeCycle = LifeCycle.TEST_CONFIG;

    private LifeCycle dataRetention = LifeCycle.TEST_CONFIG;



    public KafkaPlugin() {
    }


    public KafkaPlugin(LifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    @Override
    public LifeCycle getLifeCycle() {
        return lifeCycle;
    }


    /**
     * To be able to give the resource a name
     *
     * @param name of the Resource that is exposed for type KafkaProvider
     * @return builder
     */
    public KafkaPlugin name(final String name) {
        this.name = name;
        return this;
    }

    public KafkaPlugin keepDataAfterExit() {
        this.deleteTempDir = false;
        return this;
    }

    /**
     * To be able to give the resource a name
     *
     * @return builder
     */
    public KafkaPlugin kafkaConfigWriteMessages(int numberOfMessages) {
        initProperties();
        kafkaConfig.put("log.flush.interval.messages", String.valueOf(numberOfMessages));
        return this;
    }

    private void initProperties() {
        this.kafkaConfig = kafkaConfig == null ? new Properties() : kafkaConfig;
    }

    @RunnerPluginBeforeContainerCreation
    protected void beforeContainer(final PluginContext pluginContext) {
        if (getLifeCycle() == LifeCycle.TEST_SUITE || LifeCycle.TEST_CONFIG == getLifeCycle()) {
            createEmbeddedWithLifecycle(pluginContext);
        }
    }

    private KafkaTestServer createEmbeddedWithLifecycle(PluginContext pluginContext) {
        this.embeddedKafka = pluginLifeCycledResource.create(getLifeCycle(), pluginContext, () -> {

            Properties kafkaServerConfig = createConfig();
            KafkaTestServer kafkaTestServer = new KafkaTestServer(kafkaServerConfig);
            if (getLifeCycle() == LifeCycle.TEST_SUITE || LifeCycle.TEST_CONFIG == getLifeCycle()) {
                try {
                    startKafkaServer(kafkaTestServer);
                } catch (Exception e) {
                    throw new PluginRuntimeException("Error starting kafka server", e);
                }
            }
            return new KafkaEmbeddedServer(kafkaTestServer, kafkaServerConfig.getProperty(LOG_DIR));
        });
        return embeddedKafka.getKafkaTestServer();
    }

    private Properties createConfig() {
        Properties properties = new Properties();
        if(kafkaConfig != null) {
            if (kafkaConfig.containsKey(LOG_DIR)) {
                LOG.warn("storage is managed outside the plugin, will disable cleanup!");
                deleteTempDir = false;
            } else {
                initLogDir(properties);
            }
            kafkaConfig.forEach(properties::put);
        } else {
            initLogDir(properties);
        }
        return properties;
    }

    private void initLogDir(Properties properties) {
        String logDir = "target/kafka_testing/" + UUID.randomUUID().toString().substring(29, 36);
        LOG.info("Kafka will log to dir: '{}'", logDir);
        if(deleteTempDir) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    LOG.info("Deleting dir {}", logDir);
                    FileUtils.deleteDirectory(new File(logDir));
                } catch (IOException e) {

                }
            }));
        }
        properties.put(LOG_DIR, logDir);
    }

    @RunnerPluginBeforeClassTest
    protected void beforeTestClass(PluginContext pluginContext) throws Exception {
        if (getLifeCycle() == LifeCycle.TEST_CLASS) {
            createAndStartKafkaServer(pluginContext);
        }
    }

    @RunnerPluginAfterClassTest
    protected void afterTestClass(PluginContext pluginContext) throws Exception {
        if (getLifeCycle() == LifeCycle.TEST_CLASS) {
            stopAndCleanup();
        }
    }

    @RunnerPluginBeforeTest
    protected void beforeTest(PluginContext pluginContext) throws Exception {
        if (getLifeCycle() == LifeCycle.TEST) {
            createAndStartKafkaServer(pluginContext);
        }
    }

    @RunnerPluginAfterTest
    protected void afterTest() throws Exception {
        if (getLifeCycle() == LifeCycle.TEST) {
            stopAndCleanup();
        }
        if(dataRetention == LifeCycle.TEST){
            KafkaAdminUtil kafkaAdminUtil = new KafkaAdminUtil(embeddedKafka.getKafkaTestServer());
            kafkaAdminUtil.deleteAllData();
        }
    }


    private void createAndStartKafkaServer(PluginContext pluginContext) throws Exception {
        KafkaTestServer kafkaTestServer = createEmbeddedWithLifecycle(pluginContext);
        startKafkaServer(kafkaTestServer);
    }

    private void startKafkaServer(KafkaTestServer kafkaTestServer) throws Exception {
        kafkaTestServer.start();
        LOG.info("Started kafka server at {}", kafkaTestServer.getKafkaConnectString());
    }

    private void stopAndCleanup() throws Exception {
        LOG.info("Stopping kafka server at {}", embeddedKafka.getKafkaTestServer().getKafkaConnectString());
        embeddedKafka.getKafkaTestServer().stop();
    }

    public KafkaTestServer getEmbedded() {
        LOG.info("Getting kafka server at {}", embeddedKafka.getKafkaTestServer().getKafkaConnectString());
        return embeddedKafka.getKafkaTestServer();
    }

    @Override
    public Set<ResourceProvider> resources() {
        if (getLifeCycle() == LifeCycle.TEST || getLifeCycle() == LifeCycle.TEST_CLASS) {
            throw new IllegalArgumentException("Lifecycle can not be TEST or TEST_CLASS when connecting resources, this is because the MQ has to be started before the Resource is used");
        }
        Set<ResourceProvider> resourceProviders = new HashSet<>();
        resourceProviders.add(new ResourceProvider(CONNECTION_STRING, String.class, () -> embeddedKafka.getKafkaTestServer().getKafkaConnectString()));
        resourceProviders.add(new ResourceProvider(name, KafkaProvider.class, () -> embeddedKafka.getKafkaTestServer()));
        return resourceProviders;
    }
}
