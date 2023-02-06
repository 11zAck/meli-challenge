package com.meli.challenge.handlers;

import com.meli.challenge.clients.MeliClient;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@Log4j2
public class ProxyHandler {

    private final MeliClient client;

    public ProxyHandler(MeliClient client) {
        this.client = client;
    }

    @Retry(name="getAPIRetry")
    public Mono<ServerResponse> getAPI(ServerRequest request) {
        log.info("GET URI: {}", request.uri().getPath());

        return ServerResponse
                .ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(
                        client.get(request.uri().getPath()),
                        String.class
                );
    }

    public Mono<ServerResponse> postAPI(ServerRequest request) {
        log.info("POST URI: {}", request.uri().getPath());

        return request
                .bodyToMono(String.class)
                .flatMap( s -> client.post(request.uri().getPath(), s) )
                .flatMap( s -> ServerResponse.noContent().build() )
                .log();
    }

}
