package com.gexiang.core;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import com.google.protobuf.InvalidProtocolBufferException;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.reflection.v1alpha.ServerReflectionResponse.MessageResponseCase;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class LookupServiceHandler implements StreamObserver<ServerReflectionResponse> {
    private static final Logger logger = LoggerFactory.getLogger(LookupServiceHandler.class);
    private final SettableFuture<DescriptorProtos.FileDescriptorSet> resultFuture;
    private final String serviceName;
    private final HashSet<String> requestedDescriptors;
    private final HashMap<String, FileDescriptorProto> resolvedDescriptors;
    private StreamObserver<ServerReflectionRequest> requestStream;

    // Used to notice when we've received all the files we've asked for and we can end the rpc.
    private int outstandingRequests;

    public LookupServiceHandler(String serviceName) {
        this.serviceName = serviceName;
        this.resultFuture = SettableFuture.create();
        this.resolvedDescriptors = new HashMap<>();
        this.requestedDescriptors = new HashSet<>();
        this.outstandingRequests = 0;
    }

    ListenableFuture<DescriptorProtos.FileDescriptorSet> start(
            StreamObserver<ServerReflectionRequest> requestStream) {
        this.requestStream = requestStream;
        requestStream.onNext(requestForSymbol(serviceName));
        ++outstandingRequests;
        return resultFuture;
    }

    @Override
    public void onNext(ServerReflectionResponse response) {
        MessageResponseCase responseCase = response.getMessageResponseCase();
        switch (responseCase) {
            case FILE_DESCRIPTOR_RESPONSE:
                ImmutableSet<FileDescriptorProto> descriptors =
                        parseDescriptors(response.getFileDescriptorResponse().getFileDescriptorProtoList());
                descriptors.forEach(d -> resolvedDescriptors.put(d.getName(), d));
                descriptors.forEach(d -> processDependencies(d));
                break;
            default:
                logger.warn("Got unknown reflection response type: " + responseCase);
                break;
        }
    }

    @Override
    public void onError(Throwable t) {
        resultFuture.setException(new RuntimeException("Reflection lookup rpc failed for: " + serviceName, t));
    }

    @Override
    public void onCompleted() {
        if (!resultFuture.isDone()) {
            logger.error("Unexpected completion of the server reflection rpc");
            resultFuture.setException(new RuntimeException("Unexpected end of rpc"));
        }
    }

    private ImmutableSet<FileDescriptorProto> parseDescriptors(List<ByteString> descriptorBytes) {
        ImmutableSet.Builder<FileDescriptorProto> resultBuilder = ImmutableSet.builder();
        for (ByteString fileDescriptorBytes : descriptorBytes) {
            try {
                resultBuilder.add(FileDescriptorProto.parseFrom(fileDescriptorBytes));
            } catch (InvalidProtocolBufferException e) {
                logger.warn("Failed to parse bytes as file descriptor proto");
            }
        }
        return resultBuilder.build();
    }

    private void processDependencies(FileDescriptorProto fileDescriptor) {
        logger.debug("Processing deps of descriptor: " + fileDescriptor.getName());
        fileDescriptor.getDependencyList().forEach(dep -> {
            if (!resolvedDescriptors.containsKey(dep) && !requestedDescriptors.contains(dep)) {
                requestedDescriptors.add(dep);
                ++outstandingRequests;
                requestStream.onNext(requestForDescriptor(dep));
            }
        });

        --outstandingRequests;
        if (outstandingRequests == 0) {
            logger.debug("Retrieved service definition for [{}] by reflection", serviceName);
            resultFuture.set(FileDescriptorSet.newBuilder()
                    .addAllFile(resolvedDescriptors.values())
                    .build());
            requestStream.onCompleted();
        }
    }

    private static ServerReflectionRequest requestForDescriptor(String name) {
        return ServerReflectionRequest.newBuilder()
                .setFileByFilename(name)
                .build();
    }

    private static ServerReflectionRequest requestForSymbol(String symbol) {
        return ServerReflectionRequest.newBuilder()
                .setFileContainingSymbol(symbol)
                .build();
    }
}
