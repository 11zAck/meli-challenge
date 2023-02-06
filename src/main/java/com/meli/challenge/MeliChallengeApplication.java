package com.meli.challenge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@Configuration
@EnableCaching
@EnableAspectJAutoProxy
@SpringBootApplication
public class MeliChallengeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeliChallengeApplication.class, args);
    }

}
