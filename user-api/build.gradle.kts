import com.google.protobuf.gradle.id

plugins {
    kotlin("jvm") version "2.3.21"
    id("com.google.protobuf") version "0.10.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    `java-library`
}

group = "org.random.chat.user"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("io.grpc:grpc-stub:1.83.1")
    api("io.grpc:grpc-protobuf:1.83.1")
    api("io.grpc:grpc-kotlin-stub:1.5.0")
    api("com.google.protobuf:protobuf-java:4.35.1")
    api("com.google.protobuf:protobuf-kotlin:4.35.1")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    filter {
        exclude { element -> element.file.path.contains("/build/") }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.35.1"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.83.1"
        }
        id("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:1.5.0:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                id("kotlin")
            }
            task.plugins {
                id("grpc")
                id("grpckt")
            }
        }
    }
}
