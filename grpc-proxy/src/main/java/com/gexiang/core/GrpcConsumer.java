package com.gexiang.core;

import com.gexiang.ContextAware;
import com.gexiang.vo.GrpcContext;
import com.gexiang.vo.ProxyError;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import io.grpc.CallOptions;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.MonoSink;

import java.util.function.Consumer;

public class GrpcConsumer  implements Consumer<MonoSink<String>> {
    private static Logger logger = LoggerFactory.getLogger(GrpcConsumer.class);
    private final GrpcContext grpcContext;

    public GrpcConsumer(GrpcContext grpcContext){
        this.grpcContext = grpcContext;
    }

    @Override
    public void accept(MonoSink<String> tMonoSink) {
        GrpcClient grpcClient = ContextAware.getBean(ClientFactory.class).getClient(grpcContext.getMethod(), grpcContext.getVer());
        if(grpcClient == null){
            int value = HttpStatus.SERVICE_UNAVAILABLE.value();
            String msg    = "Find none channel";
            tMonoSink.error(new ProxyError(value, msg));
            return;
        }

        // This collects all known types into a registry for resolution of potential "Any" types.
        TypeRegistry registry = ContextAware.getBean(ClientFactory.class).getServerAny(grpcContext.getMethod(), grpcContext.getVer());
        JsonMessageHandler msgHandler = new JsonMessageHandler(registry, grpcClient.getMethdDesc().getInputType());
        DynamicMessage dmsg = msgHandler.jsonToMessage(grpcContext.getBody());
        if(dmsg == null){
            int value = HttpStatus.BAD_REQUEST.value();
            String msg    = HttpStatus.BAD_REQUEST.getReasonPhrase();
            tMonoSink.error(new ProxyError(value, msg));
            return;
        }

        StreamObserver<DynamicMessage> streamObserver = CompositeStreamObserver.of(new LoggingStatsWriter(),
                MessageWriter.create(registry, tMonoSink, grpcContext));
        grpcClient.call(dmsg, streamObserver, CallOptions.DEFAULT);
        logger.info("Call return!!!");
    }
}
