package org.hrodberaht.injection.extensions.plugin.demo.test.config;

import org.hrodberaht.injection.plugin.junit.PluggableContainerConfigBase;
import org.hrodberaht.injection.stream.InjectionRegistryBuilder;
import org.hrodberaht.injection.plugin.junit.plugins.JpaPlugin;

import javax.sql.DataSource;

/**
 * Inject extension TDD
 *
 * @author Robert Alexandersson
 *         2011-05-03 20:31
 * @created 1.0
 * @since 1.0
 */
public class CourseContainerConfigExample extends PluggableContainerConfigBase {

    public static final String DATASOURCE_NAME = "MyDataSource";


    @Override
    protected void register(InjectionRegistryBuilder registryBuilder) {
        JpaPlugin jpaPlugin = activatePlugin(JpaPlugin.class);

        DataSource dataSource = jpaPlugin.getCreator(DataSource.class).create(DATASOURCE_NAME);


        // DataSource dataSourceTwo = getCreator(DataSource.class).create("secondDataSource");

        jpaPlugin.createEntityManager("example-jpa");

        // Load schema is a custom method located in the plugin code, this creates clean separation
        jpaPlugin
                .loadSchema(dataSource, "org.hrodberaht.injection.extensions.plugin.course")
        ;


        registryBuilder
                .scan(() -> "org.hrodberaht.injection.extensions.plugin.demo.service")
                .resource(builder ->
                        builder
                                .resource("MyDataSource", DataSource.class, dataSource)

                )
        ;
    }
}