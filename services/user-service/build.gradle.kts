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
    description = "Full Spring context + H2 (see src/integrationTest)"
    group = "verification"
    useJUnitPlatform()
    testClassesDirs = sourceSets["integrationTest"].output.classesDirs
    classpath = sourceSets["integrationTest"].runtimeClasspath
    shouldRunAfter(tasks.test)
}

dependencies {
    implementation(project(":libs:common-contracts"))
    implementation(project(":libs:common-events"))
    implementation(project(":libs:common-security"))
    implementation(project(":libs:jwt-support"))
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.liquibase:liquibase-core")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    runtimeOnly("org.postgresql:postgresql")
    testRuntimeOnly("com.h2database:h2")
}
