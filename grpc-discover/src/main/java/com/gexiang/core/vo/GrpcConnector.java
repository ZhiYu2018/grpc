package com.gexiang.core.vo;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GrpcConnector implements AutoCloseable{
    private static Logger logger = LoggerFactory.getLogger(GrpcConnector.class);
    private final LinkedList<String> addrs;
    private final ConcurrentHashMap<String, ManagedChannel> channelMap;
    private int robinIndex;
    public GrpcConnector(){
        addrs = new LinkedList<>();
        channelMap = new ConcurrentHashMap<>();
        robinIndex = 0;
    }

    public ManagedChannel getChannel(){
        String address = getAddrs();
        if(address == null){
            logger.info("Get channel failed for addrs is empty");
            return null;
        }

        ManagedChannel channel = channelMap.get(address);
        if(channel != null){
            return channel;
        }

        return createChannel(address);
    }

    public int getAddrsCount(){
        return addrs.size();
    }

    public void kickOutChannel(String serverName,String address){
        logger.info("Kick out {} channel for:{}", address, serverName);
        synchronized (this){
            ManagedChannel channel = channelMap.get(address);
            if(channel != null){
                channel.shutdown();
            }
            addrs.remove(address);
        }
    }

    public void addAddress(List<String> addrList){
        synchronized (this){
            for(String str: addrList){
                if(addrs.contains(str)){
                    continue;
                }
                addrs.add(str);
            }
        }
    }

    public void addAddress(String addr){
        synchronized (this){
            if(!addrs.contains(addr)){
                addrs.add(addr);
            }
        }
    }

    private ManagedChannel createChannel(String address){
        synchronized (this){
            ManagedChannel channel = channelMap.get(address);
            if(channel != null){
                return channel;
            }
            /***connect channel***/
            logger.info("Use server:{}", address);
            String[] host = address.split(":");
            channel = ManagedChannelBuilder.forAddress(host[0], Integer.valueOf(host[1])).usePlaintext().build();
            channelMap.put(address, channel);
            return channel;
        }
    }

    private String getAddrs(){
        synchronized (this){
            if(addrs.isEmpty()){
                return null;
            }
            int ridx = robinIndex % addrs.size();
            robinIndex = (robinIndex + 1) % addrs.size();
            return addrs.get(ridx);
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (this){
            addrs.clear();
            for(ManagedChannel channel:channelMap.values()){
                channel.shutdown();
            }

            channelMap.clear();
        }
    }
}
