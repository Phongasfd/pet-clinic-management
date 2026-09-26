package com.petclinic.petservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class PetServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetServiceApplication.class, args);
    }
}
