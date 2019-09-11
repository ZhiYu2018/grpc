package com.gexiang.client;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.DescriptorProtos;
import io.grpc.Channel;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class ServerReflectionClient {
    private static final Logger logger = LoggerFactory.getLogger(ServerReflectionClient.class);
    private static final long LIST_RPC_DEADLINE_MS = 1_000;
    private static final long LOOKUP_RPC_DEADLINE_MS = 10_000;

    private final Channel channel;
    /** Returns a new reflection client using the supplied channel. */
    public static ServerReflectionClient create(Channel channel) {
        return new ServerReflectionClient(channel);
    }

    private ServerReflectionClient(Channel channel) {
        this.channel = channel;
    }

    public ListenableFuture<ImmutableList<String>> listServices() {
        ListServicesHandler rpcHandler = new ListServicesHandler();
        StreamObserver<ServerReflectionRequest> requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LIST_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

    public ListenableFuture<DescriptorProtos.FileDescriptorSet> lookupService(String serviceName) {
        LookupServiceHandler rpcHandler = new LookupServiceHandler(serviceName);
        StreamObserver<ServerReflectionRequest> requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LOOKUP_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        return rpcHandler.start(requestStream);
    }

}
