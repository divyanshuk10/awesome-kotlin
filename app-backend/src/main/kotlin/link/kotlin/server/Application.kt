@file:JvmName("Application")

package link.kotlin.server

import at.favre.lib.crypto.bcrypt.BCrypt
import at.favre.lib.crypto.bcrypt.LongPasswordStrategies
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.undertow.Undertow
import io.undertow.server.HandlerWrapper
import io.undertow.server.HttpHandler
import io.undertow.server.RoutingHandler
import io.undertow.server.handlers.PathHandler
import io.undertow.server.handlers.resource.PathResourceManager
import io.undertow.server.handlers.resource.ResourceHandler
import io.undertow.util.ChainedHandlerWrapper
import link.kotlin.server.handlers.AttachmentHandlerWrapper
import link.kotlin.server.handlers.LoginHandler
import link.kotlin.server.handlers.REQUEST_OBJECT_MAPPER
import link.kotlin.server.handlers.toUndertow
import link.kotlin.server.lifecycle.JvmShutdownManager
import link.kotlin.server.lifecycle.ShutdownManager
import link.kotlin.server.utils.logger
import org.flywaydb.core.Flyway
import org.slf4j.Logger
import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.nio.file.Paths
import java.security.SecureRandom
import javax.sql.DataSource
import kotlin.time.Duration

fun main() {
    ApplicationFactory().run()
}

open class ApplicationFactory {
    open val undertowBuilder: Undertow.Builder by lazy {
        Undertow.builder()
            .addHttpListener(9566, "0.0.0.0")
            .setHandler(rootHandler)
    }

    open val loginHandler: HttpHandler by lazy {
        LoginHandler(
            bcryptVerifier = bcryptVerifier
        ).toUndertow()
    }

    open val apiHandler: HttpHandler by lazy {
        RoutingHandler()
            .post("/login", loginHandler)
    }

    open val resourceHandler: ResourceHandler by lazy {
        ResourceHandler(
            PathResourceManager(
                Paths.get("./app-frontend/dist/"),
                1024,
                true,
                false,
                true,
                null
            )
        )
    }

    open val requestObjectMapper: ObjectMapper by lazy {
        ObjectMapper().registerKotlinModule()
    }

    open val objectMapperHandlerWrapper: HandlerWrapper by lazy {
        AttachmentHandlerWrapper(
            REQUEST_OBJECT_MAPPER,
            requestObjectMapper
        )
    }

    open val apiHandlerWrapper: HandlerWrapper by lazy {
        ChainedHandlerWrapper(
            listOf(
                objectMapperHandlerWrapper
            )
        )
    }

    open val rootHandler: HttpHandler by lazy {
        PathHandler(1024)
            .addPrefixPath("/api", apiHandlerWrapper.wrap(apiHandler))
            .addPrefixPath("/", resourceHandler)
    }

    open val undertow: Undertow by lazy {
        undertowBuilder.build()
    }

    open val shutdownHandler: ShutdownManager by lazy {
        JvmShutdownManager(
            listOf(
                undertow::stop
            )
        )
    }

    open fun run() {
        shutdownHandler.registerHook()
        flyway.migrate()
        undertow.start()
        logger.info("Server started in {}", Duration.milliseconds(runtimeMXBean.uptime))
    }

    open val runtimeMXBean: RuntimeMXBean by lazy {
        ManagementFactory.getRuntimeMXBean()
    }
    
    open val logger: Logger by lazy {
        logger<ApplicationFactory>()
    }

    open val dataSource: DataSource by lazy {
        HikariDataSource(
            HikariConfig().also {
                it.dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
                it.username = "awesome_kotlin"
                it.password = "awesome_kotlin"
                it.addDataSourceProperty("databaseName", "awesome_kotlin")
                it.addDataSourceProperty("portNumber", "9567")
                it.addDataSourceProperty("serverName", "localhost")
            }
        )
    }

    open val bcryptHasher: BCrypt.Hasher by lazy {
        BCrypt.with(
            BCrypt.Version.VERSION_2A,
            SecureRandom(),
            LongPasswordStrategies.hashSha512(BCrypt.Version.VERSION_2A)
        )
    }

    open val bcryptVerifier: BCrypt.Verifyer by lazy {
        BCrypt.verifyer()
    }

    open val flyway: Flyway by lazy {
        Flyway.configure()
            .locations("classpath:db/migration/main")
            .dataSource(dataSource)
            .load()
    }
}
