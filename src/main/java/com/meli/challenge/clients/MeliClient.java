package com.meli.challenge.clients;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;

@Component
@Log4j2
public class MeliClient {

    private final WebClient client;
    private final ObservationRegistry registry;

    private final Tracer tracer;

    public MeliClient(
            WebClient webclient,
            ObservationRegistry registry,
            Tracer tracer) {
        this.client = webclient;
        this.registry = registry;
        this.tracer = tracer;
    }

    /**
     * Método que canaliza las solicitudes GET desde el servidor actual hacia el servidor externo.
     *
     * @param uri: URI de la solicitud. (No incluye el dominio)
     * @return
     */
    public Mono<String> get(String uri) {

        Observation observation = Observation.start("webclient-get", registry);
        return Mono.just(observation).flatMap(span -> {
                    observation.scoped(() ->
                            log.info("<ACCEPTANCE_TEST> <TRACE:{}> Connecting to external server to executing a GET method",
                                    this.tracer.currentSpan().context().traceId()));
                    return this.client.get().uri(uri).accept(MediaType.APPLICATION_JSON)
                            .exchangeToMono(response -> {
                                        if (response.statusCode().is2xxSuccessful()) {
                                            return response.bodyToMono(String.class);
                                        } else {
                                            return response.createError();
                                        }
                                    }
                            )
                            .name("client-get")
                            .tap(Micrometer.observation(registry))
                            .log();
                }).doFinally(signalType -> observation.stop())
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));

    }

    /**
     * Método que canaliza las solicitudes POST desde el servidor actual hacia el servidor externo.
     *
     * @param uri:  URI de la solicitud. (No incluye el dominio)
     * @param body: Cuerpo o Payload del mensaje.
     * @return
     */
    public Mono<Void> post(String uri, Object body) {
        log.info("Connecting to external server to executing a POST method.");
        return this.client
                .post()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(body))
                .exchangeToMono(
                        clientResponse -> {
                            if (clientResponse.statusCode().is2xxSuccessful()) {
                                return clientResponse.releaseBody();
                            } else {
                                return clientResponse.createError();
                            }
                        }
                ).log();
    }
}
