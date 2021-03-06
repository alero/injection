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

package org.hrodberaht.injection.plugin.junit.resources;

import org.hrodberaht.injection.core.spi.JavaResourceCreator;
import org.hrodberaht.injection.core.spi.ResourceFactory;
import org.hrodberaht.injection.core.spi.ResourceKey;
import org.hrodberaht.injection.plugin.context.ContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PluggableResourceFactory implements ResourceFactory {


    private static final Logger LOG = LoggerFactory.getLogger(PluggableResourceFactory.class);
    private final Map<Class, Object> typedMap = new ConcurrentHashMap<>();
    private final Map<ResourceKey, Object> namedMap = new ConcurrentHashMap<>();
    private final Map<Class, JavaResourceCreator> customCreator = new ConcurrentHashMap<>();
    private final ContextManager contextManager = new ContextManager();

    public static void setPluggableResourceFactory(ResourcePluginBase pluginBase, ResourceFactory resourceFactory) {
        pluginBase.resourceFactory = resourceFactory;
    }

    public static String asContextName(ResourceKey key) {
        return key.getName();
    }

    public Map<Class, Object> getTypedMap() {
        return typedMap;
    }

    public Map<ResourceKey, Object> getNamedMap() {
        return namedMap;
    }

    @Override
    public <T> JavaResourceCreator<T> getCreator(final Class<T> type, final boolean bindToContext) {

        JavaResourceCreator<T> javaResourceCreator = customCreator.get(type);
        if (javaResourceCreator != null) {
            LOG.info("using custom Creator {} for type {}", javaResourceCreator.getClass(), type);
            return new JavaResourceCreator<T>() {
                @Override
                public T create(String name) {
                    ResourceKey key = ResourceKey.of(name, type);
                    T instance = javaResourceCreator.create(name);
                    registerInstance(instance, key, bindToContext);
                    return instance;
                }

                @Override
                public T create() {
                    T instance = javaResourceCreator.create();
                    typedMap.put(type, instance);
                    return instance;
                }

                @Override
                public T create(String name, T instance) {
                    ResourceKey key = ResourceKey.of(name, type);
                    registerInstance(instance, key, bindToContext);
                    return instance;
                }

                @Override
                public T create(T instance) {
                    typedMap.put(type, instance);
                    return instance;
                }

                @Override
                public Class getType() {
                    return type;
                }
            };
        }
        LOG.info("using regular Creator for type {}", type);
        return new JavaResourceCreator<T>() {
            @Override
            public T create(String name) {
                ResourceKey key = ResourceKey.of(name, type);
                if (namedMap.get(key) != null) {
                    throw new RuntimeException("key value already registered for " + key.toString());
                }
                T instance = getInstance();
                registerInstance(instance, key, bindToContext);
                return instance;
            }

            private T getInstance() {
                try {
                    return type.newInstance();
                } catch (InstantiationException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public T create() {
                if (typedMap.get(type) != null) {
                    throw new RuntimeException("key value already registered for " + type.getName());
                }
                T instance = getInstance();
                typedMap.put(type, instance);
                return instance;
            }

            @Override
            public T create(String name, T instance) {
                ResourceKey key = ResourceKey.of(name, type);
                if (namedMap.get(key) != null) {
                    throw new RuntimeException("instance is already registered for " + key.toString());
                }
                registerInstance(instance, key, bindToContext);
                return instance;
            }

            @Override
            public T create(T instance) {
                if (typedMap.get(type) != null) {
                    throw new RuntimeException("key value already registered for " + type.getName());
                }
                typedMap.put(type, instance);
                return instance;
            }

            @Override
            public Class getType() {
                return type;
            }
        };
    }

    @Override
    public <T> void addResourceCrator(JavaResourceCreator<T> javaResourceCreator) {
        customCreator.put(javaResourceCreator.getType(), javaResourceCreator);
    }

    private <T> void registerInstance(T instance, ResourceKey key, boolean bindToContext) {
        namedMap.put(key, instance);
        if (bindToContext) {
            putToContext(key, instance);
        }
    }

    private <T> void putToContext(ResourceKey key, T instance) {
        contextManager.bind(asContextName(key), instance);
    }

}
