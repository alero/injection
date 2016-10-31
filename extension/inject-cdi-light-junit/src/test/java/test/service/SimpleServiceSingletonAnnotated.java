package test.service;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

@Singleton
public class SimpleServiceSingletonAnnotated {

    private String initData = null;

    @PostConstruct
    public void init() {
        initData = "initialized";
    }

    public String getInitData() {
        return initData;
    }
}
