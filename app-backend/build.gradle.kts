plugins {
    application
    kotlin("jvm")
}

application {
    mainClass.set("link.kotlin.server.Application")
    applicationName = "awesome"
}

repositories {
    mavenCentral()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs = freeCompilerArgs + "-Xopt-in=kotlin.time.ExperimentalTime"
    }
}

dependencies {
    implementation(stdlib)
    implementation(reflect)
    implementation(coroutines)

    implementation("io.undertow:undertow-core:2.2.10.Final")
    implementation("org.flywaydb:flyway-core:7.13.0")
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("com.zaxxer:HikariCP:5.0.0")
    implementation("at.favre.lib:bcrypt:0.9.0")

    implementation(jacksonXml)
    implementation(jacksonKotlin)
    implementation(jacksonJsr310)

    implementation("org.slf4j:slf4j-api:$slf4jVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

    testImplementation(mockk)
    testImplementation(junitApi)
    testRuntimeOnly(junitEngine)
}
