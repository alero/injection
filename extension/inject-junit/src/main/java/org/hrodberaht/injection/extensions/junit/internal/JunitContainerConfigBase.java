package org.hrodberaht.injection.extensions.junit.internal;

import org.hrodberaht.injection.config.InjectionRegisterScanBase;
import org.hrodberaht.injection.config.jpa.JPAContainerConfigBase;
import org.hrodberaht.injection.extensions.junit.util.EntityManagerHolder;
import org.hrodberaht.injection.internal.InjectionContainerManager;
import org.hrodberaht.injection.internal.ScopeContainer;
import org.hrodberaht.injection.register.RegistrationModuleAnnotation;
import org.hrodberaht.injection.spi.ResourceCreator;

import javax.persistence.EntityManager;
import java.util.Collection;

/**
 * Created by alexbrob on 2016-03-01.
 */
public abstract class JunitContainerConfigBase<T extends InjectionRegisterScanBase> extends JPAContainerConfigBase<T> {

    @Override
    protected ResourceCreator createResourceCreator() {
        return new ProxyResourceCreator();
    }

    public ResourceCreator<EntityManager, DataSourceProxy> getResourceCreator() {
        return resourceCreator;
    }

    public void addSQLSchemas(String schemaName, String packageBase) {
        DataSourceExecution sourceExecution = new DataSourceExecution(resourceCreator);
        if (!sourceExecution.isInitiated(schemaName, schemaName)) {
            sourceExecution.addSQLSchemas(schemaName, packageBase);
        }
    }

    public void addSQLSchemas(String testPackageName, String schemaName, String packageBase) {
        DataSourceExecution sourceExecution = new DataSourceExecution(resourceCreator);
        if (!sourceExecution.isInitiated(testPackageName, schemaName)) {
            sourceExecution.addSQLSchemas(schemaName, packageBase);
        }
    }

    @Override
    public void addSingletonActiveRegistry() {
        super.addSingletonActiveRegistry();

        addSingletonActiveEntityManagers();
    }

    private void addSingletonActiveEntityManagers() {
        Collection<EntityManager> entityManagers = getEntityManagers();
        if(entityManagers != null) {
            activeRegister.register(new RegistrationModuleAnnotation() {
                                        @Override
                                        public void registrations() {
                                            register(EntityManagerHolder.class)
                                                    .scopeAs(ScopeContainer.Scope.SINGLETON)
                                                    .registerTypeAs(InjectionContainerManager.RegisterType.FINAL)
                                                    .withInstance(new EntityManagerHolder(entityManagers));
                                        }
                                    }
            );
        }
    }
}
