package ru.dobrovichek.identity.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI identityOpenApi(
            @Value("${dobrovichek.public-api-url:http://localhost:8080}") String publicApiUrl
    ) {
        return new OpenAPI()
                .info(new Info()
                        .title("Dobrovichek Identity API")
                        .version("v1"))
                .servers(List.of(new Server()
                        .url(publicApiUrl)
                        .description("Через API Gateway")));
    }
}
