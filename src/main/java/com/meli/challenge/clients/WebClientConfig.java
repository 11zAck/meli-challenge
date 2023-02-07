package com.meli.challenge.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient webClient(WebClient.Builder builder, @Value("${routes.server}") String serverUrl) {
        return builder.baseUrl(serverUrl).build();
    }


}
