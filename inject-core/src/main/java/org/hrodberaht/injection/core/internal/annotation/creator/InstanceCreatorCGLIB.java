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

package org.hrodberaht.injection.core.internal.annotation.creator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Java Utils - Container
 * <p/>
 * There are no imports to net.sf.cglib,
 * all usage is direct and this means that this class can be loaded without cglib present.
 *
 * @author Robert Alexandersson
 * 2010-jun-05 23:25:22
 * @version 1.0
 * @since 1.0
 */
public class InstanceCreatorCGLIB implements InstanceCreator {

    private static final Map<Constructor, net.sf.cglib.reflect.FastConstructor>
            CACHED_CONSTRUCTS = new HashMap<Constructor, net.sf.cglib.reflect.FastConstructor>();
    // use fully-qualified names so imports don't need preprocessor statements
    private static final net.sf.cglib.core.NamingPolicy NAMING_POLICY
            = new net.sf.cglib.core.DefaultNamingPolicy() {
        @Override
        protected String getTag() {
            return "CreatedBySimpleInjection";
        }
    };

    private static net.sf.cglib.reflect.FastConstructor findFastCreatorInstance(Constructor constructor) {
        if (!CACHED_CONSTRUCTS.containsKey(constructor)) {
            Class classToConstruct = constructor.getDeclaringClass();
            final net.sf.cglib.reflect.FastConstructor fastConstructor
                    = newFastClass(classToConstruct)
                    .getConstructor(constructor);
            CACHED_CONSTRUCTS.put(constructor, fastConstructor);
            return fastConstructor;
        }
        return CACHED_CONSTRUCTS.get(constructor);
    }

    private static net.sf.cglib.reflect.FastClass newFastClass(Class type) {
        net.sf.cglib.reflect.FastClass.Generator generator
                = new net.sf.cglib.reflect.FastClass.Generator();
        generator.setType(type);
        generator.setClassLoader(getClassLoader(type));
        generator.setNamingPolicy(NAMING_POLICY);
        return generator.create();
    }

    private static ClassLoader getClassLoader(Class<?> type) {
        return type.getClassLoader();
    }

    @Override
    public Object createInstance(Constructor constructor, Object... parameters) {
        net.sf.cglib.reflect.FastConstructor
                fastConstructor = findFastCreatorInstance(constructor);
        try {
            return fastConstructor.newInstance(parameters);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }


    }

}
