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

        Server prodServer = new Server();
        prodServer.setUrl(serverUrl);
        prodServer.setDescription("Production Server");

        // HTTP와 HTTPS 둘 다 지원
        Server httpServer = new Server();
        httpServer.setUrl("http://localhost:8080");
        httpServer.setDescription("Local HTTP Server");

        Server httpsServer = new Server();
        httpsServer.setUrl("https://localhost:8080");
        httpsServer.setDescription("Local HTTPS Server");

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Heilous API")
                                .description(
                                        "Heilous 토지 중개 플랫폼 REST API 문서"
                                )
                                .version("v1.0.0")
                )
                .servers(List.of(prodServer, httpServer, httpsServer))
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