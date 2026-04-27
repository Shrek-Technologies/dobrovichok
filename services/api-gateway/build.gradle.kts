sourceSets {
    create("integrationTest") {
        java.srcDir("src/integrationTest/java")
        resources.srcDirs("src/integrationTest/resources", "src/test/resources")
    }
}

configurations.named("integrationTestImplementation") {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named("integrationTestRuntimeOnly") {
    extendsFrom(configurations.testRuntimeOnly.get())
}

sourceSets.named("integrationTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
}

tasks.register<Test>("integrationTest") {
    description = "Full Spring context (see src/integrationTest)"
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.test)
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.3")
    }
}

dependencies {
    implementation(project(":libs:jwt-support"))
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")
    runtimeOnly("org.webjars:webjars-locator-core:0.59")
}
