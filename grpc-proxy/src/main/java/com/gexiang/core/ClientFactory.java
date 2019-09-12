package com.gexiang.core;

import com.gexiang.vo.ProtoMethodName;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.util.JsonFormat;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.reflection.v1alpha.ServerReflectionGrpc;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.protobuf.Descriptors.MethodDescriptor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import com.google.protobuf.util.JsonFormat.TypeRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
public class ClientFactory {
    private static final long LOOKUP_RPC_DEADLINE_MS = 10_000;
    private static Logger logger = LoggerFactory.getLogger(ClientFactory.class);
    private ConcurrentMap<String, ServiceResolver> serverDescriptorMap;

    @Autowired
    public ClientFactory(Environment env){
        serverDescriptorMap = new ConcurrentHashMap<>();
        GrpcChannelPool.getPool().init(env);
        GrpcConManger.getInstance().init(env);
    }

    public GrpcClient getClient(String fullName, String ver){
        /***com.server/route**/
        ProtoMethodName grpcMethodName = ProtoMethodName.parseFullGrpcMethodName(fullName);
        ManagedChannel channel = GrpcChannelPool.getPool().getChannel(grpcMethodName.getFullServiceName(), ver);
        if(channel == null){
            logger.warn("Find channel for {}:{} failed", fullName, ver);
            return null;
        }

        Descriptors.MethodDescriptor methodDescriptor = getMethodDescriptor(grpcMethodName, ver, channel);
        if(methodDescriptor == null){
            logger.warn("Find none method descriptor for :{}", grpcMethodName.getFullServiceName());
            return null;
        }

        return new GrpcClient(methodDescriptor, channel);
    }

    public TypeRegistry getServerAny(String fullName, String ver){
        ProtoMethodName grpcMethod = ProtoMethodName.parseFullGrpcMethodName(fullName);
        String key = String.format("%s/%s", grpcMethod.getFullServiceName(), ver);
        ServiceResolver serviceResolver = serverDescriptorMap.get(key);
        if(serviceResolver == null){
            logger.warn("Get type registry for {} failed", fullName);
            return null;
        }

        TypeRegistry registry = JsonFormat.TypeRegistry.newBuilder().add(serviceResolver.listMessageTypes()).build();
        return registry;
    }

    private MethodDescriptor getMethodDescriptor(ProtoMethodName grpcMethod, String ver, Channel channel){
        String key = String.format("%s/%s", grpcMethod.getFullServiceName(), ver);
        ServiceResolver serviceResolver = serverDescriptorMap.get(key);
        if(serviceResolver == null) {
            synchronized (this) {
                serviceResolver = serverDescriptorMap.get(key);
                if (serviceResolver == null) {
                    LookupServiceHandler rpcHandler = new LookupServiceHandler(grpcMethod.getFullServiceName());
                    StreamObserver<ServerReflectionRequest> requestStream = ServerReflectionGrpc.newStub(channel)
                            .withDeadlineAfter(LOOKUP_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                            .serverReflectionInfo(rpcHandler);
                    ListenableFuture<DescriptorProtos.FileDescriptorSet> future = rpcHandler.start(requestStream);
                    try {
                        DescriptorProtos.FileDescriptorSet fileDescriptorSet = future.get();
                        serviceResolver = ServiceResolver.fromFileDescriptorSet(fileDescriptorSet);
                        serverDescriptorMap.put(key, serviceResolver);
                    } catch (Throwable t) {
                        logger.error("Get file descroptorset for {}, exeption:{}", grpcMethod.getFullServiceName(), t);
                        return null;
                    }//
                }//if
            }// end sy
        }//end if
        if(serviceResolver == null){
            return null;
        }

        MethodDescriptor methodDescriptor = serviceResolver.resolveServiceMethod(grpcMethod);
        return methodDescriptor;
    }

}
