package com.meli.challenge.clients;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class MeliClient {

    private final WebClient client;

    public MeliClient(WebClient.Builder builder, @Value("${routes.server}") String serverUrl) {
        this.client = builder.baseUrl(serverUrl).build();
    }

    public Mono<String> get(String uri) {
        return this.client.get().uri(uri).accept(MediaType.APPLICATION_JSON)
                .exchangeToMono(response -> {
                            if (response.statusCode().equals(HttpStatus.OK)) {
                                return response.bodyToMono(String.class);
                            } else {
                                return response.createError();
                            }
                        }
                )
                .log()
                .retry(3);
    }

    public Mono<Void> post(String uri, Object body) {
        return this.client
                .post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .exchangeToMono(
                        clientResponse -> {
                            if( clientResponse.statusCode().is2xxSuccessful()) {
                                return clientResponse.releaseBody();
                            }else{
                                return clientResponse.createError();
                            }
                        }
                ).log();
    }
}
