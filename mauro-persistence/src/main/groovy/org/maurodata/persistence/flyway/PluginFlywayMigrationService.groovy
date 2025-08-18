package org.maurodata.persistence.flyway

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.context.event.StartupEvent
import io.micronaut.core.annotation.Order
import io.micronaut.core.order.Ordered
import io.micronaut.data.connection.annotation.Connectable
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.flywaydb.core.Flyway

import javax.sql.DataSource

@Singleton
@CompileStatic
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
class PluginFlywayMigrationService implements ApplicationEventListener<StartupEvent> {

    final static String CORE_MIGRATIONS_FOLDER = "core"

    @Inject
    ApplicationContext applicationContext

    @Inject
    @Named("default")
    DataSource dataSource

    @Override
    void onApplicationEvent(final StartupEvent event) {
        log.trace("Loading data at startup")
        runPluginMigrations()
    }

    @Connectable
    void runPluginMigrations() {
        log.trace("🔄 Scanning for plugin migrations...")

        ScanResult scanResult = new ClassGraph().acceptPaths("db/migration").scan()

        Set<String> pluginDirs = [] as Set<String>

        scanResult.getAllResources().each {resource ->
            String path = resource.getPath()
            if (path.startsWith("db/migration/")) {
                String[] segments = path.split("/")
                if (segments.length > 2) {
                    pluginDirs.add(segments[2]) // e.g., "user" from db/migration/user/V1__init.sql
                }
            }
        }

        runMigration(CORE_MIGRATIONS_FOLDER)
        pluginDirs.each { pluginName ->
            if(pluginName != CORE_MIGRATIONS_FOLDER) {
                runMigration(pluginName)
            }
        }

    }

    void runMigration(String pluginName) {
        try{
            String location = "classpath:db/migration/" + pluginName
            String tableName = "flyway_history_" + pluginName

            log.info(String.format("🚀 Running Flyway migration for plugin '%s' (%s, schema %s, table: %s)%n", pluginName, location, pluginName, tableName))

            Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations(location)
                .table(tableName)
                .schemas(pluginName)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()

            flyway.migrate()

        } catch (Exception e) {
            throw new RuntimeException("Failed to run plugin migrations", e)
        }

    }

}