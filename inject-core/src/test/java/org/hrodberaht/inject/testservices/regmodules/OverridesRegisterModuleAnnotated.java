package org.hrodberaht.inject.testservices.regmodules;

import org.hrodberaht.inject.testservices.annotated.Spare;
import org.hrodberaht.inject.testservices.annotated.Tire;
import org.hrodberaht.injection.register.Overrides;
import org.hrodberaht.injection.register.RegistrationModuleAnnotation;

/**
 * Simple Java Utils
 *
 * @author Robert Alexandersson
 *         2010-aug-01 16:32:48
 * @version 1.0
 * @since 1.0
 */
@Overrides(className = RegisterModuleAnnotated.class)
public class OverridesRegisterModuleAnnotated extends RegistrationModuleAnnotation {

    @Override
    public void registrations() {
        register(Tire.class).annotated(Spare.class).with(Tire.class);
    }
}