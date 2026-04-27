plugins {
    id("java")
    id("org.springframework.boot") version "3.3.5" apply false
    id("io.spring.dependency-management") version "1.1.6" apply false
}

group = "ru.dobrovichek"
version = "0.1.0-SNAPSHOT"

tasks.register("integrationTest") {
    description = "Runs src/integrationTest for services that define it"
    group = "verification"
    dependsOn(
        project(":services:api-gateway").tasks.named("integrationTest"),
        project(":services:identity-service").tasks.named("integrationTest"),
        project(":services:notification-service").tasks.named("integrationTest"),
        project(":services:request-service").tasks.named("integrationTest"),
        project(":services:user-service").tasks.named("integrationTest"),
    )
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    repositories {
        mavenCentral()
    }
}

val javaVersion = JavaVersion.VERSION_21

configure(subprojects.filter { it.path.startsWith(":libs:") }) {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    tasks.withType<org.gradle.api.tasks.testing.Test> {
        useJUnitPlatform()
    }
}

configure(subprojects.filter { it.path.startsWith(":services:") }) {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    dependencies {
        "implementation"("org.springframework.boot:spring-boot-starter-actuator")
        "implementation"("org.springframework.boot:spring-boot-starter-validation")
        "testImplementation"("org.springframework.boot:spring-boot-starter-test")
        "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<org.gradle.api.tasks.testing.Test> {
        useJUnitPlatform()
    }
}

configure(subprojects.filter { it.path.startsWith(":libs:") || it.path.startsWith(":services:") }) {
    apply(plugin = "jacoco")

    configure<org.gradle.testing.jacoco.plugins.JacocoPluginExtension> {
        toolVersion = "0.8.12"
    }

    tasks.named<org.gradle.testing.jacoco.tasks.JacocoReport>("jacocoTestReport") {
        dependsOn(tasks.named("test"))
        reports {
            xml.required.set(true)
            html.required.set(true)
            html.outputLocation.set(layout.buildDirectory.dir("coverage/jacoco-html"))
            xml.outputLocation.set(layout.buildDirectory.file("coverage/jacoco.xml"))
        }
    }

    tasks.named<org.gradle.api.tasks.testing.Test>("test") {
        finalizedBy(tasks.named("jacocoTestReport"))
    }
}
