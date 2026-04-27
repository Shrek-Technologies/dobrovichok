plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.6"
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.5")
    }
}

dependencies {
    api(project(":libs:common-contracts"))

    api("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")

    api("org.springframework.security:spring-security-oauth2-jose")
    api("org.springframework.security:spring-security-config")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
