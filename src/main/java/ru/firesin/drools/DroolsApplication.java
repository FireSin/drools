package ru.firesin.drools;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DroolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroolsApplication.class, args);
        System.out.println("=== Drools Spring Boot Application Started ===");
        System.out.println("Swagger UI: http://localhost:8080/swagger-ui.html");
        System.out.println("Тестовые endpoints:");
        System.out.println("- GET  /api/drools/info");
        System.out.println("- GET  /api/drools/test/25");
        System.out.println("- POST /api/drools/static");
        System.out.println("- POST /api/drools/kiehelper");
        System.out.println("- POST /api/drools/dynamic");
    }

}
