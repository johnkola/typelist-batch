package com.bazarbozorg.typelistservice.batch.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for Swagger/OpenAPI documentation.
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:TypeList Batch Service}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Creates the OpenAPI configuration.
     *
     * @return OpenAPI configuration
     */
    @Bean
    public OpenAPI typeListBatchOpenAPI() {
        return new OpenAPI()
                .info(new Info().title(applicationName + " API")
                        .description("REST API for the TypeList Batch Processing Service")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Bazar Bozorg")
                                .email("support@bazarbozorg.com")
                                .url("https://www.bazarbozorg.com"))
                        .license(new License().name("Proprietary").url("https://www.bazarbozorg.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("TypeList Service Documentation")
                        .url("https://docs.bazarbozorg.com/typelist-service"))
                .servers(List.of(
                        new Server().url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server().url("https://api.bazarbozorg.com")
                                .description("Production API server")));
    }
}