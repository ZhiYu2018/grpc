package com.gexiang.core;

import com.gexiang.ContextAware;
import com.gexiang.vo.ProxyResponse;
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
    private final String fullName;
    private final String ver;
    private final String body;

    public GrpcConsumer(String fullName, String ver, String body){
        this.fullName = fullName;
        this.ver = ver;
        this.body = body;
    }

    @Override
    public void accept(MonoSink<String> tMonoSink) {
        GrpcClient grpcClient = ContextAware.getBean(ClientFactory.class).getClient(fullName, ver);
        if(grpcClient == null){
            int value = HttpStatus.SERVICE_UNAVAILABLE.value();
            String msg    = "Find none channel";
            tMonoSink.success(new ProxyResponse(value, msg).toJson());
            return;
        }

        // This collects all known types into a registry for resolution of potential "Any" types.
        TypeRegistry registry = ContextAware.getBean(ClientFactory.class).getServerAny(fullName, ver);
        JsonMessageHandler msgHandler = new JsonMessageHandler(registry, grpcClient.getMethdDesc().getInputType());
        DynamicMessage dmsg = msgHandler.jsonToMessage(body);
        if(dmsg == null){
            int value = HttpStatus.BAD_REQUEST.value();
            String msg    = HttpStatus.BAD_REQUEST.getReasonPhrase();
            tMonoSink.success(new ProxyResponse(value, msg).toJson());
            return;
        }

        StreamObserver<DynamicMessage> streamObserver = CompositeStreamObserver.of(new LoggingStatsWriter(),
                MessageWriter.create(registry, tMonoSink));
        grpcClient.call(dmsg, streamObserver, CallOptions.DEFAULT);
        logger.info("Call return!!!");
    }
}
