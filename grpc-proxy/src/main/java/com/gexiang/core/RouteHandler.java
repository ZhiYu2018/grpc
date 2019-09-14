package com.gexiang.core;

import com.gexiang.vo.ConstValues;
import com.gexiang.vo.GrpcContext;
import com.gexiang.vo.ProxyError;
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
        List<String> method = request.getHeaders().get(ConstValues.X_GRPC_METHOD);
        if(method == null || method.isEmpty()){
            int value = HttpStatus.BAD_REQUEST.value();
            String msg    = HttpStatus.BAD_REQUEST.getReasonPhrase();
            return Mono.error(new ProxyError(value, msg));
        }

        List<String> vers = request.getHeaders().get(ConstValues.X_GRPC_VER);
        if(vers == null || vers.isEmpty()){
            int value = HttpStatus.BAD_REQUEST.value();
            String msg    = HttpStatus.BAD_REQUEST.getReasonPhrase();
            return Mono.error(new ProxyError(value, msg));
        }

        GrpcContext grpcContext = GrpcContext.newBuilder().setBody(body.getBody()).setMethod(method.get(0))
                .setVer(vers.get(0)).setSessionInfo(GrpcConManger.getInstance().removeSession()).build();
        logger.debug("Session:{},Contentype:{},Method:{}, ver:{}", grpcContext.getSessionInfo().getSid(),
                     request.getHeaders().getContentType().toString(), grpcContext.getMethod(), grpcContext.getVer());
        return GrpcConManger.getInstance().limitRequest(grpcContext);
    }

}
