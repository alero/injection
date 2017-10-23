package org.hrodberaht.injection.extensions.plugin.junit.datasource.test.config;

import org.hrodberaht.injection.plugin.junit.ContainerContextConfigBase;
import org.hrodberaht.injection.plugin.junit.plugins.DataSourcePlugin;
import org.hrodberaht.injection.stream.InjectionRegistryBuilder;

import javax.sql.DataSource;

/**
 * Inject extension TDD
 *
 * @author Robert Alexandersson
 * 2011-05-03 20:31
 * @created 1.0
 * @since 1.0
 */
public class SQLLoadingContainerConfig extends ContainerContextConfigBase {


    @Override
    public void register(InjectionRegistryBuilder registryBuilder) {

        DataSourcePlugin dataSourcePlugin = activatePlugin(DataSourcePlugin.class);
        DataSource dataSource = dataSourcePlugin.getCreator(DataSource.class).create("MyDataSource");

        // Load schema is a custom method located in the plugin code, this creates clean separation
        dataSourcePlugin
                .loadSchema(dataSource, "org.hrodberaht.injection.extensions.plugin.course2")
        // .loadSchema(dataSource, "org.hrodberaht.injection.extensions.plugin.course2")
        ;


        registryBuilder
                .scan(() -> "org.hrodberaht.injection.extensions.plugin.junit.datasource.service")
                .resource(builder ->
                        builder
                                .resource("MyDataSource", DataSource.class, dataSource)
                                .resource(DataSource.class, dataSource)
                )
        ;
    }


}
