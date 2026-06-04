package com.heilous.global.config;

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
public class SwaggerConfig {

    @Value("${server.url:https://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {

        SecurityScheme bearerScheme =
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .name("Authorization");

        SecurityRequirement securityRequirement =
                new SecurityRequirement()
                        .addList("bearerAuth");

        Server server = new Server();
        server.setUrl(serverUrl);
        server.setDescription("API Server");

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Heilous API")
                                .description(
                                        "Heilous 토지 중개 플랫폼 REST API 문서"
                                )
                                .version("v1.0.0")
                )
                .servers(List.of(server))
                .addSecurityItem(securityRequirement)
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        "bearerAuth",
                                        bearerScheme
                                )
                );
    }
}