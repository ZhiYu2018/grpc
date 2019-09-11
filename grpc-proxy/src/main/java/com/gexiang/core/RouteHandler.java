package com.gexiang.core;

import com.gexiang.vo.Constent;
import com.gexiang.vo.ProxyResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import reactor.core.publisher.Mono;
import java.util.List;


public class RouteHandler {
    private static Logger logger = LoggerFactory.getLogger(RouteHandler.class);

    public static Mono<String> handleRoute(ServerHttpRequest request, HttpEntity<String> body){
        List<String> method = request.getHeaders().get(Constent.X_GRPC_METHOD);
        if(method == null || method.isEmpty()){
            int value = HttpStatus.BAD_REQUEST.value();
            String msg    = HttpStatus.BAD_REQUEST.getReasonPhrase();
            return Mono.just(new ProxyResponse(value, msg).toJson());
        }

        List<String> vers = request.getHeaders().get(Constent.X_GRPC_VER);
        String ver = "v1.0";
        if(vers != null && !vers.isEmpty()){
            ver = vers.get(0);
        }

        logger.info("Contentype:{},Method:{}, ver:{}", request.getHeaders().getContentType().toString(),
                method.get(0), ver);
        return Mono.create(new GrpcConsumer(method.get(0), ver, body.getBody()));
    }
}
