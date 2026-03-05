package com.example.scheduler;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2;

@SpringBootApplication
public class SchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchedulerApplication.class, args);
    }

    @Bean
    Customizer<HttpSecurity> httpSecurityCustomizer(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri
    ) {
        return security -> security
                .with(mcpServerOAuth2(), a -> a
                        .authorizationServer(issuerUri));
    }
}

@Service
class DogAdoptionScheduler {


    @McpTool(description = "schedule an appointment to pick up or adopt " +
            "a dog from a Pooch Palace location ")
    String scheduleAdoption(
            @McpToolParam(description = "id of the dog to adopt") int dogId,
            @McpToolParam(description = "name of the dog to adopt") String dogName) {
        var i = Instant
                .now()
                .plus(3, ChronoUnit.DAYS)
                .toString();
        IO.println("schedulign " + dogId + '/' + dogName + " for " + i);
        return i;
    }

}
