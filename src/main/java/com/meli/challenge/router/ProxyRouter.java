package com.meli.challenge.router;

import com.meli.challenge.handlers.ProxyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration(proxyBeanMethods = false)
public class ProxyRouter {
    @Bean
    public RouterFunction<ServerResponse> route(ProxyHandler proxyHandler) {

        return RouterFunctions
                .route(GET("/**").and(accept(MediaType.APPLICATION_JSON)), proxyHandler::getAPI)
                .andRoute(POST("/**").and(accept(MediaType.APPLICATION_FORM_URLENCODED)), proxyHandler::postAPI);
    }
}
