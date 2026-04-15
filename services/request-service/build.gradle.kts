dependencies {
    implementation(project(":libs:common-contracts"))
    implementation(project(":libs:common-events"))
    implementation(project(":libs:common-security"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
}
