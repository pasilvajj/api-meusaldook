package com.economy.finance.config;

import com.economy.finance.security.JwtProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeOpenApi() {
        final String scheme = "bearerAuth";
        return new OpenAPI()
                .info(
                        new Info()
                                .title("Finance API")
                                .description("API REST v1 para gestão financeira pessoal")
                                .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(scheme))
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        scheme,
                                        new SecurityScheme()
                                                .name(scheme)
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")));
    }
}
