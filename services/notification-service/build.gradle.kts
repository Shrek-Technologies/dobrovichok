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

dependencies {
    implementation(project(":libs:common-events"))
    implementation(project(":libs:common-security"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("com.google.firebase:firebase-admin:9.4.1")

    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
