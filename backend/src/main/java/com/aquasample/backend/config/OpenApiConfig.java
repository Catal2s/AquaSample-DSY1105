package com.aquasample.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Titulo y descripcion que se muestran en Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI aquaSampleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("AquaSample API")
                .version("1.0.0")
                .description("Microservicio de muestras de choritos (caso ALDEMAR, DSY1105). "
                        + "Las operaciones de escritura exigen el header X-Rol: OPERADOR, SUPERVISOR o ANALISTA. "
                        + "Todos los datos son ficticios."));
    }
}
