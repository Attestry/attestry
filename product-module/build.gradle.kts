plugins {
    `java-library`
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:4.0.3"))
    implementation(project(":user-auth-module"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
