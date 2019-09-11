package com.gexiang.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.23.0)",
    comments = "Source: grpc_gate.proto")
public final class GrpcGateGrpc {

  private GrpcGateGrpc() {}

  public static final String SERVICE_NAME = "com.gexiang.grpc.GrpcGate";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.gexiang.grpc.GrpcRequest,
      com.gexiang.grpc.GrpcResponse> getRouteMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Route",
      requestType = com.gexiang.grpc.GrpcRequest.class,
      responseType = com.gexiang.grpc.GrpcResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.gexiang.grpc.GrpcRequest,
      com.gexiang.grpc.GrpcResponse> getRouteMethod() {
    io.grpc.MethodDescriptor<com.gexiang.grpc.GrpcRequest, com.gexiang.grpc.GrpcResponse> getRouteMethod;
    if ((getRouteMethod = GrpcGateGrpc.getRouteMethod) == null) {
      synchronized (GrpcGateGrpc.class) {
        if ((getRouteMethod = GrpcGateGrpc.getRouteMethod) == null) {
          GrpcGateGrpc.getRouteMethod = getRouteMethod =
              io.grpc.MethodDescriptor.<com.gexiang.grpc.GrpcRequest, com.gexiang.grpc.GrpcResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Route"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gexiang.grpc.GrpcRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.gexiang.grpc.GrpcResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GrpcGateMethodDescriptorSupplier("Route"))
              .build();
        }
      }
    }
    return getRouteMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GrpcGateStub newStub(io.grpc.Channel channel) {
    return new GrpcGateStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GrpcGateBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new GrpcGateBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GrpcGateFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new GrpcGateFutureStub(channel);
  }

  /**
   */
  public static abstract class GrpcGateImplBase implements io.grpc.BindableService {

    /**
     */
    public void route(com.gexiang.grpc.GrpcRequest request,
        io.grpc.stub.StreamObserver<com.gexiang.grpc.GrpcResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRouteMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getRouteMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.gexiang.grpc.GrpcRequest,
                com.gexiang.grpc.GrpcResponse>(
                  this, METHODID_ROUTE)))
          .build();
    }
  }

  /**
   */
  public static final class GrpcGateStub extends io.grpc.stub.AbstractStub<GrpcGateStub> {
    private GrpcGateStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GrpcGateStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GrpcGateStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GrpcGateStub(channel, callOptions);
    }

    /**
     */
    public void route(com.gexiang.grpc.GrpcRequest request,
        io.grpc.stub.StreamObserver<com.gexiang.grpc.GrpcResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getRouteMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class GrpcGateBlockingStub extends io.grpc.stub.AbstractStub<GrpcGateBlockingStub> {
    private GrpcGateBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GrpcGateBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GrpcGateBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GrpcGateBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.gexiang.grpc.GrpcResponse route(com.gexiang.grpc.GrpcRequest request) {
      return blockingUnaryCall(
          getChannel(), getRouteMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class GrpcGateFutureStub extends io.grpc.stub.AbstractStub<GrpcGateFutureStub> {
    private GrpcGateFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private GrpcGateFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GrpcGateFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new GrpcGateFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.gexiang.grpc.GrpcResponse> route(
        com.gexiang.grpc.GrpcRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRouteMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ROUTE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GrpcGateImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GrpcGateImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ROUTE:
          serviceImpl.route((com.gexiang.grpc.GrpcRequest) request,
              (io.grpc.stub.StreamObserver<com.gexiang.grpc.GrpcResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class GrpcGateBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GrpcGateBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.gexiang.grpc.GrpcGateProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GrpcGate");
    }
  }

  private static final class GrpcGateFileDescriptorSupplier
      extends GrpcGateBaseDescriptorSupplier {
    GrpcGateFileDescriptorSupplier() {}
  }

  private static final class GrpcGateMethodDescriptorSupplier
      extends GrpcGateBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GrpcGateMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GrpcGateGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GrpcGateFileDescriptorSupplier())
              .addMethod(getRouteMethod())
              .build();
        }
      }
    }
    return result;
  }
}
