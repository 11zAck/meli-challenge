package com.meli.challenge.aspects;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import io.micrometer.tracing.Tracer;
import lombok.extern.log4j.Log4j2;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.stereotype.Component;
import reactor.core.observability.micrometer.Micrometer;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Objects;

@Log4j2
@Aspect
@Component
public class CacheAspect {

    private final ObservationRegistry registry;
    private final Tracer tracer;
    private final Scheduler scheduler = Schedulers.boundedElastic();
    private final Cache<String, Object> cache;

    public CacheAspect(EmbeddedCacheManager cacheManager, Tracer tracer, ObservationRegistry registry) {
        cache = cacheManager.getCache("meliCache");
        this.tracer = tracer;
        this.registry = registry;
    }

    @Around("execution(* com.meli.challenge.clients.MeliClient.get(..)) && args(uri,..)")
    public Mono<Object> findResponseAspect(ProceedingJoinPoint joinPoint, String uri) {

        Observation observation = Observation.start("cache", registry);

        return Mono
                .just(observation)
                .flatMap(span -> {
                    observation
                            .scoped(() -> log.info("<ACCEPTANCE_TEST> <TRACE:{}> Find Response in Cache",
                                this.tracer.currentSpan().context().traceId())
                            );
                    return Mono
                            .fromSupplier(() -> getItem(uri))
                            .subscribeOn(scheduler)
                            .switchIfEmpty(Mono.defer(() -> {
                                Mono<Object> data;
                                try {
                                    data = (Mono<Object>) joinPoint.proceed();
                                } catch (Throwable throwable) {
                                    throw new RuntimeException(throwable);
                                }
                                return data
                                        .flatMap(info -> Mono
                                                .fromSupplier(() -> putInCache(uri, info))
                                                .subscribeOn(scheduler)
                                        );
                            }))
                            .name("cache-find")
                            .tap(Micrometer.observation(registry))
                            .log();
                })
                .doFinally(signalType -> observation.stop())
                .contextWrite(context -> context.put(ObservationThreadLocalAccessor.KEY, observation));

    }

    private Object putInCache(String id, Object info) {
        log.info("putting a new Object into the cache");
        cache.put(id, info);
        return info;
    }

    private Object getItem(String id) {
        log.info("looking a Object in cache");
        Object item = cache.get(id);
        if (Objects.nonNull(item)) {
            log.info("Object found in cache");
            return item;
        }
        log.info("Object not in cache");
        return null;
    }
}
