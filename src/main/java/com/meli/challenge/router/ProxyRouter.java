package com.meli.challenge.router;

import com.meli.challenge.handlers.ProxyHandler;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import lombok.extern.log4j.Log4j2;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration(proxyBeanMethods = false)
@Log4j2
public class ProxyRouter {
    private final ObservationRegistry registry;
    private final Tracer tracer;


    public ProxyRouter(ObservationRegistry registry, Tracer tracer) {
        this.registry = registry;
        this.tracer = tracer;

    }

    @Bean
    public RouterFunction<ServerResponse> route(ProxyHandler proxyHandler) {

        return RouterFunctions
                .route(GET("/**").and(accept(MediaType.APPLICATION_JSON)), proxyHandler::getAPI)
                .andRoute(POST("/**").and(accept(MediaType.APPLICATION_FORM_URLENCODED)), proxyHandler::postAPI)
                .filter((request, next) -> {

                    Observation observation = Observation.start("cache", registry);

                    return Mono.just(observation).flatMap(span -> {
                                observation.scoped(() ->
                                        log.info("<ACCEPTANCE_TEST> <TRACE:{}> FILTER HTTP",
                                        this.tracer.currentSpan().context().traceId()));

                                span.highCardinalityKeyValue("user.ip",
                                        request.remoteAddress().isPresent() ?
                                        request.remoteAddress().get().toString() : "NO_IP"
                                );

                                return next.handle(request);
                            })
                            .doFinally(signalType -> observation.stop())
                            .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));
                })
                ;
    }
}
