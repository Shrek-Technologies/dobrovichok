dependencies {
    implementation(project(":libs:common-contracts"))
    implementation(project(":libs:common-security"))
    implementation(project(":libs:jwt-support"))
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    runtimeOnly("org.postgresql:postgresql")
}
