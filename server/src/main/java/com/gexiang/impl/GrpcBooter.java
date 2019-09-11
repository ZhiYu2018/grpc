package com.gexiang.impl;

import com.gexiang.core.GrpcRegister;
import com.gexiang.grpc.GrpcGateGrpc;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GrpcBooter {
    private static Logger logger = LoggerFactory.getLogger(GrpcBooter.class);
    private Server[] servers;

    @Autowired
    public GrpcBooter(Environment env){
        String port = env.getProperty("grpc.gate.port");
        logger.info("Start to grpc gate, port is:{}", port);
        GrpcRegister.grpcRegister().init(env);
        try{
            servers = new Server[4];
            for(int i = 0; i < 4; i++) {
                int p = Integer.valueOf(port) + i;
                servers[i] = ServerBuilder.forPort(p)
                        .addService(new GrpcGateImpl())
                        .addService(ProtoReflectionService.newInstance()).build().start();
                GrpcRegister.grpcRegister().register(GrpcGateGrpc.SERVICE_NAME, String.valueOf(p), "v1.0");
            }

            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");
                    logger.info("*** shutting down gRPC proxy since JVM is shutting down");
                    try {
                        GrpcBooter.this.stop();
                        GrpcRegister.grpcRegister().close();
                    }catch (Throwable t){

                    }
                    System.err.println("*** server shut down");
                    logger.info("*** server shut down");
                }
            });
        }catch (Throwable t){
            logger.error("Start grpc gate failed:", t);
        }
    }


    private void stop(){
        logger.info("Grpc gate is quit ......");
        if (servers != null) {
            for(int i = 0; i < servers.length; i++) {
                if(servers[i] != null) {
                    servers[i].shutdown();
                    servers[i] = null;
                }
            }
        }
    }
}
