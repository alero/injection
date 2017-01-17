package org.hrodberaht.injection.extensions.junit.demo.test;

import org.hrodberaht.injection.extensions.junit.ContainerContext;
import org.hrodberaht.injection.extensions.junit.JUnitRunner;
import org.hrodberaht.injection.extensions.junit.demo.test.config.CourseContainerConfigExample;
import org.junit.runner.RunWith;

import javax.annotation.PostConstruct;

@ContainerContext(CourseContainerConfigExample.class)
@RunWith(JUnitRunner.class)
public abstract class AbstractBaseClass {

    protected String init = null;

    @PostConstruct
    protected void init() {
        init = "initiated";
    }

}