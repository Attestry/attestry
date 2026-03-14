import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.net.URI

plugins {
    base
}

group = "io.attestry"
version = "0.0.1-SNAPSHOT"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        val githubActor = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        val githubToken = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))

        if (githubActor.isPresent && githubToken.isPresent) {
            maven {
                name = "GitHubPackages"
                url = URI("https://maven.pkg.github.com/Attestry/attestry")
                credentials {
                    username = githubActor.get()
                    password = githubToken.get()
                }
            }
        }
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
        }
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }
}
