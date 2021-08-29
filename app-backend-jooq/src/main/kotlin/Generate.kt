import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Target
import java.util.*

fun main() {
    GenerationTool.generate(Configuration().apply {
        jdbc = Jdbc().apply {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:9567/awesome_kotlin"
            user = "awesome_kotlin"
            password = "awesome_kotlin"
        }

        generator = Generator().apply {
            name = "org.jooq.codegen.KotlinGenerator"
            database = Database().apply {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                includes = ".*"
                inputSchema = "public"
            }

            generate = Generate().apply {
                isDaos = true
                isPojosAsKotlinDataClasses = true
            }

            target = Target().apply {
                packageName = "link.kotlin.server.jooq.main"
                directory = "./app-backend/src/main/kotlin"
                locale = Locale.ROOT.toLanguageTag()
            }
        }
    })
}
