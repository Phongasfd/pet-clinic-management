package com.petclinic.petservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// No local users: identity comes from the gateway headers (see security.SecurityConfig),
// so Spring Boot's generated in-memory user/password is not wanted.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableJpaAuditing
public class PetServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PetServiceApplication.class, args);
    }
}
