plugins {
    kotlin("jvm") version("2.0.0")
    `java-gradle-plugin`
    `maven-publish`

    id("dev.deftu.gradle.bloom") version("0.1.0")
}

group = "dev.deftu"
version = "0.1.0"

java.withSourcesJar()

repositories {
    mavenCentral()
    mavenLocal()
}

bloom {
    replacement("@PROJECT_NAME@", project.name)
    replacement("@PROJECT_VERSION@", project.version.toString())
    replacement("@SECRET@", "haha no fuck you")
}

dependencies {
    implementation(kotlin("gradle-plugin"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin-api:2.0.0")
    implementation("com.google.guava:guava:30.1.1-jre")
}

gradlePlugin {
    plugins {
        register("bloom") {
            id = "dev.deftu.gradle.bloom"
            implementationClass = "dev.deftu.gradle.bloom.BloomPlugin"
        }
    }
}

publishing {
    val publishingUsername: String? = run {
        return@run project.findProperty("deftu.publishing.username")?.toString() ?: System.getenv("DEFTU_PUBLISHING_USERNAME")
    }

    val publishingPassword: String? = run {
        return@run project.findProperty("deftu.publishing.password")?.toString() ?: System.getenv("DEFTU_PUBLISHING_PASSWORD")
    }

    repositories {
        mavenLocal()
        if (publishingUsername != null && publishingPassword != null) {
            fun MavenArtifactRepository.applyCredentials() {
                authentication.create<BasicAuthentication>("basic")
                credentials {
                    username = publishingUsername
                    password = publishingPassword
                }
            }

            maven {
                name = "DeftuReleases"
                url = uri("https://maven.deftu.dev/releases")
                applyCredentials()
            }

            maven {
                name = "DeftuSnapshots"
                url = uri("https://maven.deftu.dev/snapshots")
                applyCredentials()
            }
        }
    }
}
