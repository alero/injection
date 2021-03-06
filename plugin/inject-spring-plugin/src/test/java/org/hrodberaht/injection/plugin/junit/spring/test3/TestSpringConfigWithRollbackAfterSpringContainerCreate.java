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

package org.hrodberaht.injection.plugin.junit.spring.test3;

import org.hrodberaht.injection.plugin.junit.ContainerContext;
import org.hrodberaht.injection.plugin.junit.JUnit4Runner;
import org.hrodberaht.injection.plugin.junit.spring.config.JUnitConfigExampleResourceToSpringBeansRollbackOnSpringContainerCreate;
import org.hrodberaht.injection.plugin.junit.spring.testservices2.SpringBeanWithSpringBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@ContainerContext(JUnitConfigExampleResourceToSpringBeansRollbackOnSpringContainerCreate.class)
@RunWith(JUnit4Runner.class)
public class TestSpringConfigWithRollbackAfterSpringContainerCreate {

    @Autowired
    private SpringBeanWithSpringBean springBean;

    @Test
    public void testWiredBeanResource() throws Exception {
        assertNotNull(springBean);
        assertNotNull(springBean.getName("dude"));
    }

    @Test
    public void testWiredBeanRollbackOnInit() throws Exception {

        assertNotNull(springBean);

        assertNull(springBean.getName("init"));


    }

}
