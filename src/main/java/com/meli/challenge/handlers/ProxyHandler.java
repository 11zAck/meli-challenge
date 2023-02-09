package com.meli.challenge.handlers;

import com.meli.challenge.clients.MeliClient;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@Log4j2
public class ProxyHandler {

    private final ObservationRegistry registry;

    private final Tracer tracer;
    private final MeliClient client;
    private final Bucket bucket;

    public ProxyHandler(MeliClient client, ObservationRegistry registry, Tracer tracer) {
        this.client = client;
        this.registry = registry;
        this.tracer = tracer;
        Bandwidth limit = Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(1)));
        this.bucket = Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Método para manipular, filtrar y generar estadísticas para los GET.
     *
     * @param request: Objeto que contiene información de la solicitud
     * @return La respuesta del servidor (Mono: 0 o 1)
     */
    public Mono<ServerResponse> getAPI(ServerRequest request) {
        log.info("GET URI: {}", request.uri().getPath());

//        if(bucket.tryConsume(1)){
            if (request.uri().getPath().contains("prometheus")
                    || request.uri().getPath().contains("actuator")
                    || request.uri().getPath().contains("favicon"))
                return Mono.empty();

            Observation observation = Observation.start("http-get", registry);
            return Mono.just(observation).flatMap(span -> {
                        observation.scoped(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Handler GET methods",
                                this.tracer.currentSpan().context().traceId()));
                        return ServerResponse
                                .ok()
                                .contentType(MediaType.APPLICATION_JSON)
                                .body(
                                        client.get(request.uri().getPath()),
                                        String.class
                                )
                                .name("webapi-client")
                                .tap(Micrometer.observation(registry))
                                .log();
                    }).doFinally(signalType -> observation.stop())
                    .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
//        }
//
//        return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS).build();


    }

    /**
     * Método para manipular, filtrar y generar estadísticas para los POST.
     *
     * @param request: Objeto que contiene información de la solicitud
     * @return La respuesta del servidor (Mono: 0 o 1)
     */
    @Retry(name = "postAPIRetry")
    public Mono<ServerResponse> postAPI(ServerRequest request) {
        log.info("POST URI: {}", request.uri().getPath());

        if (request.uri().getPath().contains("prometheus")
                || request.uri().getPath().contains("actuator")
                || request.uri().getPath().contains("favicon"))
            return Mono.empty();

        return request
                .bodyToMono(String.class)
                .flatMap(s -> client.post(request.uri().getPath(), s))
                .flatMap(s -> ServerResponse.noContent().build())
                .log();
    }

}
