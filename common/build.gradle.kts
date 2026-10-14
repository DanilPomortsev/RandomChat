plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    `java-library`
}

group = "org.random.chat"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(platform("org.springframework.boot:spring-boot-dependencies:4.1.1"))
    api("ru.yoomoney.tech:db-queue:15.1.0")
    api("org.springframework:spring-context")
    api("org.springframework:spring-jdbc")
    api("org.springframework:spring-tx")
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property",
        )
    }
}
