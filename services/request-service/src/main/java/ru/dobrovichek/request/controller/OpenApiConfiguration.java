package ru.dobrovichek.request.controller;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI requestServiceOpenApi(
            @Value("${dobrovichek.public-api-url:http://localhost:8080}") String publicApiUrl
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("Dobrovichek Request Service API")
                        .version("v1"))
                .servers(List.of(new Server()
                        .url(publicApiUrl)
                        .description("Через API Gateway")))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }
}
