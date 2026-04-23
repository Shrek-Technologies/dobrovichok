package ru.dobrovichek.request.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.dobrovichek.security.ServiceHeaders;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI requestServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Dobrovichek Request Service API")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(ServiceHeaders.USER_ID, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ServiceHeaders.USER_ID))
                        .addSecuritySchemes(ServiceHeaders.USER_ROLE, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(ServiceHeaders.USER_ROLE)))
                .addSecurityItem(new SecurityRequirement()
                        .addList(ServiceHeaders.USER_ID)
                        .addList(ServiceHeaders.USER_ROLE));
    }
}
