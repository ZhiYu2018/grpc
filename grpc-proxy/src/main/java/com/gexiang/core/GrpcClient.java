package com.gexiang.core;

import com.gexiang.io.CompositeStreamObserver;
import com.gexiang.io.DoneObserver;
import com.gexiang.io.ProxyMarshaller;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.DynamicMessage;
import io.grpc.CallOptions;
import io.grpc.ClientCall;
import io.grpc.MethodDescriptor.MethodType;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.Channel;
import io.grpc.stub.ClientCalls;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcClient {
    private static final Logger logger = LoggerFactory.getLogger(GrpcClient.class);
    private final MethodDescriptor methdDesc;
    private final Channel channel;

    public static GrpcClient create(MethodDescriptor descriptor, Channel channel){
        return new GrpcClient(descriptor, channel);
    }

    GrpcClient(MethodDescriptor protoMethodDescriptor, Channel channel) {
        this.methdDesc = protoMethodDescriptor;
        this.channel = channel;
    }

    public MethodDescriptor getMethdDesc(){
        return this.methdDesc;
    }

    /**
     * Makes an rpc to the remote endpoint and respects the supplied callback. Returns a future which
     * terminates once the call has ended. For calls which are single-request, this throws
     * {@link IllegalArgumentException} if the size of {@code requests} is not exactly 1.
     */
    public ListenableFuture<Void> call(DynamicMessage request, StreamObserver<DynamicMessage> responseObserver,
                                       CallOptions callOptions) {
        MethodType methodType = getMethodType();
        if (methodType == MethodType.UNARY) {
            return callUnary(request, responseObserver, callOptions);
        } else if (methodType == MethodType.SERVER_STREAMING) {
            return callServerStreaming(request, responseObserver, callOptions);
        } else if (methodType == MethodType.CLIENT_STREAMING) {
            return callClientStreaming(request, responseObserver, callOptions);
        } else {
            return callBidiStreaming(request, responseObserver, callOptions);
        }
    }

    private ListenableFuture<Void> callBidiStreaming(DynamicMessage request, StreamObserver<DynamicMessage> responseObserver,
                                                     CallOptions callOptions) {
        DoneObserver<DynamicMessage> doneObserver = new DoneObserver<>();
        StreamObserver<DynamicMessage> requestObserver = ClientCalls.asyncBidiStreamingCall(
                createCall(callOptions), CompositeStreamObserver.of(responseObserver, doneObserver));
        responseObserver.onNext(request);
        requestObserver.onCompleted();
        return doneObserver.getCompletionFuture();
    }

    private ListenableFuture<Void> callClientStreaming(DynamicMessage request, StreamObserver<DynamicMessage> responseObserver,
                                                       CallOptions callOptions) {
        DoneObserver<DynamicMessage> doneObserver = new DoneObserver<>();
        StreamObserver<DynamicMessage> requestObserver = ClientCalls.asyncClientStreamingCall(
                createCall(callOptions), CompositeStreamObserver.of(responseObserver, doneObserver));
        responseObserver.onNext(request);
        requestObserver.onCompleted();
        return doneObserver.getCompletionFuture();
    }

    private ListenableFuture<Void> callServerStreaming(DynamicMessage request,
            StreamObserver<DynamicMessage> responseObserver,
            CallOptions callOptions) {
        DoneObserver<DynamicMessage> doneObserver = new DoneObserver<>();
        ClientCalls.asyncServerStreamingCall(createCall(callOptions), request,
                CompositeStreamObserver.of(responseObserver, doneObserver));
        return doneObserver.getCompletionFuture();
    }

    private ListenableFuture<Void> callUnary(
            DynamicMessage request,
            StreamObserver<DynamicMessage> responseObserver,
            CallOptions callOptions) {
        DoneObserver<DynamicMessage> doneObserver = new DoneObserver<>();
        ClientCalls.asyncUnaryCall(createCall(callOptions), request,
                CompositeStreamObserver.of(responseObserver, doneObserver));
        return doneObserver.getCompletionFuture();
    }

    private ClientCall<DynamicMessage, DynamicMessage> createCall(CallOptions callOptions) {
        return channel.newCall(createGrpcMethodDescriptor(), callOptions);
    }

    private io.grpc.MethodDescriptor<DynamicMessage, DynamicMessage> createGrpcMethodDescriptor() {
        Marshaller<DynamicMessage> reqMarshaller = new ProxyMarshaller(methdDesc.getInputType());
        Marshaller<DynamicMessage> rspMarshaller = new ProxyMarshaller(methdDesc.getOutputType());
        return io.grpc.MethodDescriptor.<DynamicMessage,DynamicMessage>newBuilder().setFullMethodName(getFullMethodName())
                .setType(getMethodType()).setRequestMarshaller(reqMarshaller).setResponseMarshaller(rspMarshaller).build();
    }

    private String getFullMethodName() {
        String serviceName = methdDesc.getService().getFullName();
        String methodName = methdDesc.getName();
        return io.grpc.MethodDescriptor.generateFullMethodName(serviceName, methodName);
    }

    /** Returns the appropriate method type based on whether the client or server expect streams. */
    private MethodType getMethodType() {
        boolean clientStreaming = methdDesc.toProto().getClientStreaming();
        boolean serverStreaming = methdDesc.toProto().getServerStreaming();

        if (!clientStreaming && !serverStreaming) {
            return MethodType.UNARY;
        } else if (!clientStreaming && serverStreaming) {
            return MethodType.SERVER_STREAMING;
        } else if (clientStreaming && !serverStreaming) {
            return MethodType.CLIENT_STREAMING;
        } else {
            return MethodType.BIDI_STREAMING;
        }
    }

}
