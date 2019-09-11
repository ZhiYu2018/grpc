package com.gexiang.client;

import com.gexiang.server.GxNameResolverFactory;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GxClient {
    private static Logger logger = LoggerFactory.getLogger(GxClient.class);
    private volatile ManagedChannel channel;
    public GxClient(String uri, String police){
        channel = ManagedChannelBuilder.forTarget(uri).nameResolverFactory(new GxNameResolverFactory())
                 .defaultLoadBalancingPolicy(police).usePlaintext().build();
    }

    public ManagedChannel getChannel(){
        return channel;
    }

    public void stop(){
        if(channel != null){
            channel.shutdownNow();
            channel = null;
        }
    }
}
