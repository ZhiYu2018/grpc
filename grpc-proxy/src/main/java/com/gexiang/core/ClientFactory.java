package com.gexiang.core;

import com.gexiang.protobuf.LookupServiceHandler;
import com.gexiang.protobuf.ProtocInvoker;
import com.gexiang.protobuf.ServiceResolver;
import com.gexiang.vo.ConstValues;
import com.gexiang.vo.ProtoMethodName;
import com.google.common.collect.ImmutableList;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Component
public class ClientFactory {
    private static final long LOOKUP_RPC_DEADLINE_MS = 10_000;
    private static Logger logger = LoggerFactory.getLogger(ClientFactory.class);
    private String grpcRootPath;
    private ImmutableList<Path> protocIncludePaths;
    private ConcurrentMap<String, ServiceResolver> serverDescriptorMap;
    @Autowired
    public ClientFactory(Environment env){
        grpcRootPath = env.getProperty(ConstValues.GRPC_ROOT_PATH);
        String includePath = env.getProperty(ConstValues.GRPC_INCLUDE_PATH);
        ImmutableList.Builder<Path> includePaths = ImmutableList.builder();
        logger.info("Grpc root {}, include path:{}", grpcRootPath, includePath);
        if((includePath != null) && grpcRootPath != null){
            String [] paths = includePath.split(",");
            for(String p:paths){
                Path path = Paths.get(grpcRootPath + "/" + p);
                if(Files.exists(path)){
                    logger.info("Path {}/{} exists", grpcRootPath, p);
                    includePaths.add(path.toAbsolutePath());
                }
            }
        }

        protocIncludePaths = includePaths.build();
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
                    /**get from ref **/
                    DescriptorProtos.FileDescriptorSet fileDescriptorSet = getReflectionDescriptors(grpcMethod, ver, channel);
                    if(fileDescriptorSet == null) {
                        /**if failed load from file**/
                        fileDescriptorSet = getProtoDescriptors(grpcMethod, ver);
                    }
                    serviceResolver = ServiceResolver.fromFileDescriptorSet(fileDescriptorSet);
                    serverDescriptorMap.put(key, serviceResolver);
                }//if
            }// end sy
        }//end if
        if(serviceResolver == null){
            return null;
        }

        MethodDescriptor methodDescriptor = serviceResolver.resolveServiceMethod(grpcMethod);
        return methodDescriptor;
    }

    private DescriptorProtos.FileDescriptorSet getProtoDescriptors(ProtoMethodName grpcMethod, String ver){
        if(grpcRootPath != null) {
            String root = String.format("%s/%s/%s", grpcRootPath, grpcMethod.getFullServiceName(),ver);
            ProtocInvoker protocInvoker = new ProtocInvoker(Paths.get(root), protocIncludePaths);
            try{
                return protocInvoker.invoke();
            }catch (Throwable t){
                logger.error("Get proto for {} exception {}", grpcMethod.getServiceName(), t.getMessage());
                return null;
            }
        }
        return null;
    }

    private DescriptorProtos.FileDescriptorSet getReflectionDescriptors(ProtoMethodName grpcMethod, String ver, Channel channel){
        LookupServiceHandler rpcHandler = new LookupServiceHandler(grpcMethod.getFullServiceName());
        StreamObserver<ServerReflectionRequest> requestStream = ServerReflectionGrpc.newStub(channel)
                .withDeadlineAfter(LOOKUP_RPC_DEADLINE_MS, TimeUnit.MILLISECONDS)
                .serverReflectionInfo(rpcHandler);
        ListenableFuture<DescriptorProtos.FileDescriptorSet> future = rpcHandler.start(requestStream);
        try {
            DescriptorProtos.FileDescriptorSet fileDescriptorSet = future.get();
            return fileDescriptorSet;
        } catch (Throwable t) {
            logger.error("Get file descroptorset for {}, exeption:{}", grpcMethod.getFullServiceName(), t);
            return null;
        }//
    }
}
