plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "user-service"

include(":common")
project(":common").projectDir = file("../common")

include(":user-api")
project(":user-api").projectDir = file("../user-api")
