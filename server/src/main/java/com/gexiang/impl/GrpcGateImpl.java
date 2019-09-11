package com.gexiang.impl;

import com.gexiang.grpc.GrpcGateGrpc;
import com.gexiang.grpc.GrpcRequest;
import com.gexiang.grpc.GrpcResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcGateImpl extends GrpcGateGrpc.GrpcGateImplBase{
    private static Logger logger = LoggerFactory.getLogger(GrpcGateImpl.class);

    public GrpcGateImpl(){

    }
    public void route(GrpcRequest request, StreamObserver<GrpcResponse> responseObserver) {
        logger.info("Get from:{}", request.getMethod());
        GrpcResponse response = GrpcResponse.newBuilder().setCode("200").setMsg("ok").build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
