package org.hrodberaht.injection.plugin.junit;

import org.hrodberaht.injection.internal.InjectionContainerManager;
import org.hrodberaht.injection.register.InjectionRegister;
import org.hrodberaht.injection.register.RegistrationModule;
import org.hrodberaht.injection.register.RegistrationModuleAnnotation;

import javax.inject.Inject;

/**
 * Unit Test JUnit (using @Inject)
 *
 * @author Robert Alexandersson
 * 2010-okt-13 00:23:43
 * @version 1.0
 * @since 1.0
 * <p/>
 */
public class ContainerLifeCycleTestUtil {

    @Inject
    private InjectionRegister module;


    public void registerServiceInstance(Class serviceDefinition, Object service) {
        RegistrationModuleAnnotation registrationModule = new RegistrationModuleAnnotation() {
            @Override
            public void registrations() {
                register(serviceDefinition)
                        .registerTypeAs(InjectionContainerManager.RegisterType.OVERRIDE_NORMAL)
                        .withInstance(service);
            }
        };
        module.register(registrationModule);
    }

    public void registerServiceInstance(Class serviceDefinition, String qualifier, Object service) {
        RegistrationModuleAnnotation registrationModule = new RegistrationModuleAnnotation() {
            @Override
            public void registrations() {
                register(serviceDefinition)
                        .registerTypeAs(InjectionContainerManager.RegisterType.OVERRIDE_NORMAL)
                        .named(qualifier)
                        .withInstance(service);
            }
        };
        module.register(registrationModule);
    }

    public void registerModule(RegistrationModule module) {
        this.module.register(module);
    }


    public <T> T getService(Class<T> aClass) {
        return module.getContainer().get(aClass);
    }

    public <T> T getService(Class<T> aClass, String name) {
        return module.getContainer().get(aClass, name);
    }
}