package com.aquasample.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Microservicio de muestras de AquaSample.
 * Guarda las muestras que envia la app Android y permite revisarlas por rol.
 */
@SpringBootApplication
public class AquaSampleBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AquaSampleBackendApplication.class, args);
    }
}
