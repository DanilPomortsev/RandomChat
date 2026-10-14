plugins {
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    id("org.jooq.jooq-codegen-gradle") version "3.21.7"
}

group = "org.random.chat.user.service"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))
    implementation(project(":user-api"))
    implementation("org.springframework.boot:spring-boot-starter-grpc-server")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    runtimeOnly("org.postgresql:postgresql")
    jooqCodegen("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-grpc-server-test")
    testImplementation("org.springframework.boot:spring-boot-starter-grpc-client-test")
    testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
    testImplementation("org.liquibase:liquibase-core")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
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

jooq {
    configuration {
        jdbc {
            driver = "org.postgresql.Driver"
            url = "jdbc:postgresql://localhost:5432/user_service"
            user = "user_service_app"
            password = "user_service_app"
        }
        generator {
            name = "org.jooq.codegen.KotlinGenerator"
            database {
                name = "org.jooq.meta.postgres.PostgresDatabase"
                inputSchema = "public"
                excludes = "databasechangelog|databasechangeloglock|queue_tasks"
                forcedTypes {
                    forcedType {
                        userType = "org.random.chat.user.service.domain.UserStatus"
                        converter = "org.random.chat.user.service.domain.UserStatusConverter"
                        includeExpression = ".*\\.USERS\\.STATUS"
                    }
                    forcedType {
                        userType = "org.random.chat.user.service.domain.AvatarStatus"
                        converter = "org.random.chat.user.service.domain.AvatarStatusConverter"
                        includeExpression = ".*\\.USER_AVATARS\\.STATUS"
                    }
                }
            }
            generate {
                isPojos = true
                isPojosAsKotlinDataClasses = true
                isDaos = true
                isSpringAnnotations = false
                isSpringDao = false
                isKotlinNotNullPojoAttributes = true
                isKotlinNotNullRecordAttributes = true
            }
            target {
                packageName = "org.random.chat.user.service.jooq"
                directory = layout.buildDirectory.dir("generated-src/jooq/main").get().asFile.path
            }
        }
    }
}

sourceSets.named("main") {
    kotlin.srcDir(layout.buildDirectory.dir("generated-src/jooq/main"))
}

tasks.named("compileKotlin") {
    dependsOn(tasks.named("jooqCodegen"))
}

tasks.test {
    useJUnitPlatform()
    val dockerHost = System.getenv("DOCKER_HOST") ?: resolveDockerHost()
    if (!dockerHost.isNullOrBlank()) {
        environment("DOCKER_HOST", dockerHost)
    }
    if (
        System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE") == null &&
        dockerHost.orEmpty().contains("/.colima/")
    ) {
        environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")
    }
}

fun resolveDockerHost(): String? =
    runCatching {
        val process = ProcessBuilder(
            "docker",
            "context",
            "inspect",
            "--format",
            "{{.Endpoints.docker.Host}}",
        ).redirectError(ProcessBuilder.Redirect.DISCARD).start()
        if (process.waitFor() != 0) {
            return null
        }
        process.inputStream.bufferedReader().readText().trim().ifBlank { null }
    }.getOrNull()
