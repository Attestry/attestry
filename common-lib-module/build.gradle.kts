plugins {
    `java-library`
    `maven-publish`
}

java {
    withSourcesJar()
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.3"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

publishing {
    publications {
        create<MavenPublication>("commonLib") {
            groupId = project.group.toString()
            artifactId = "attestry-common-lib"
            version = project.version.toString()
            from(components["java"])

            pom {
                name.set("Attestry Common Library")
                description.set("Shared contracts and base abstractions for Attestry modules")
            }
        }
    }

    repositories {
        val githubActor = providers.gradleProperty("gpr.user")
            .orElse(providers.environmentVariable("GITHUB_ACTOR"))
        val githubToken = providers.gradleProperty("gpr.key")
            .orElse(providers.environmentVariable("GITHUB_TOKEN"))

        if (githubActor.isPresent && githubToken.isPresent) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/attestry/attestry")
                credentials {
                    username = githubActor.get()
                    password = githubToken.get()
                }
            }
        }
    }
}
