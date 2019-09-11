package com.gexiang.controller;


import com.gexiang.core.RouteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class GrpcController {
    private static Logger logger = LoggerFactory.getLogger(GrpcController.class);

    @GetMapping("/route")
    public Mono<String> GetRoute(ServerHttpRequest request, HttpEntity<String> body){
        logger.info("URL is:{}, body:{}", request.getURI().toString(),
                body.getBody());
        return RouteHandler.handleRoute(request, body);
    }

    @PostMapping("/route")
    public Mono<String> PostRoute(ServerHttpRequest request, HttpEntity<String> body){
        logger.info("URL is:{}, body:{}", request.getURI().toString(),
                body.getBody());
        return RouteHandler.handleRoute(request, body);
    }

}
