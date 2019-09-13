package com.gexiang.core;

import com.google.common.base.Charsets;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class EtcdData implements AutoCloseable{
    public static String SERVER_PREFIX = "grpc.io";
    public static String PROP_ETCD_HOST = "etcd.host";
    private static final Logger logger = LoggerFactory.getLogger(EtcdData.class);
    private volatile Client etcdclient;
    private String hostList;
    public EtcdData(String hostList){
        logger.info("Etcd host is:{}", hostList);
        this.hostList = hostList;
        etcdclient = Client.builder().endpoints(hostList.split(";")).build();
    }

    public LeaseGrantResponse reconnect(){
        try{
            close();
        }catch (Throwable t){
            logger.info("Reconnect exception:{}", t.getMessage());
        }

        etcdclient = Client.builder().endpoints(hostList.split(";")).build();
        return getLeaseId();
    }


    public void put(String key, String value){
        if(etcdclient == null){
            logger.error("Client is null");
        }
        ByteSequence bsKey = ByteSequence.from(key, Charsets.UTF_8);
        ByteSequence bsValue = ByteSequence.from(value, Charsets.UTF_8);
        CompletableFuture<PutResponse> future = etcdclient.getKVClient().put(bsKey, bsValue);
        try {
            PutResponse rsp = future.get();
            logger.info("Get rsp:{}", rsp.getPrevKv().getVersion());
        }catch (Throwable t){
            logger.warn("Put key {} exceptions:{}", key, t.getMessage());
        }
    }

    public int put(String key, String value, PutOption option){
        if(etcdclient == null){
            logger.error("Client is null");
            return -1;
        }
        ByteSequence bsKey = ByteSequence.from(key, Charsets.UTF_8);
        ByteSequence bsValue = ByteSequence.from(value, Charsets.UTF_8);
        CompletableFuture<PutResponse> future = etcdclient.getKVClient().put(bsKey, bsValue, option);
        try {
            PutResponse rsp = future.get();
            return 0;
        }catch (Throwable t){
            if(t instanceof StatusRuntimeException){
                StatusRuntimeException sre = (StatusRuntimeException)t;
                logger.error("Put key {} failed:{}", key, sre.getStatus().getCode().name());
            }
            logger.warn("Put key {} exceptions:{}", key, t.getMessage());
        }
        return -1;
    }

    public String get(String key){
        if(etcdclient == null){
            logger.error("Client is null");
            return null;
        }
        ByteSequence bsKey = ByteSequence.from(key, Charsets.UTF_8);
        CompletableFuture<GetResponse> future = etcdclient.getKVClient().get(bsKey);
        try{
            GetResponse rsp = future.get();
            if(!rsp.getKvs().isEmpty()){
                logger.info("Get key:{} count:{}", key, rsp.getCount());
                return rsp.getKvs().get(0).getValue().toString(Charsets.UTF_8);
            }
            logger.info("Find none such key:{}", key);
            return null;
        }catch (Throwable t){
            logger.warn("Get key {} exceptions:{}", key, t.getMessage());
            return null;
        }
    }

    public List<KeyValue> listPreKey(String preKey){
        if(etcdclient == null){
            logger.error("Client is null");
            return null;
        }
        ByteSequence bsKey = ByteSequence.from(preKey, Charsets.UTF_8);
        GetOption option = GetOption.newBuilder().withPrefix(bsKey).build();
        CompletableFuture<GetResponse> future = etcdclient.getKVClient().get(bsKey, option);
        try{
            GetResponse rsp = future.get();
            List<KeyValue> kvs = new LinkedList<>();
            logger.info("Get count:{}", rsp.getCount());
            kvs.addAll(rsp.getKvs());
            return kvs;
        }catch (Throwable t){
            logger.warn("Get key {} exceptions:{}", preKey, t.getMessage());
            return null;
        }
    }


    public LeaseGrantResponse getLeaseId(){
        if(etcdclient == null){
            logger.error("Client is null");
            return null;
        }
        CompletableFuture<LeaseGrantResponse> future = etcdclient.getLeaseClient().grant(0L);
        try{
            LeaseGrantResponse rsp = future.get(3, TimeUnit.SECONDS);
            logger.info("grant lease id {}, ttl {}", rsp.getID(), rsp.getTTL());
            return rsp;
        }catch (Throwable t){
            logger.warn("grant lease id exceptions:{}", t.getMessage());
        }

        return null;
    }

    public CloseableClient keepLeaseObserver(long leaseId, StreamObserver<LeaseKeepAliveResponse> observer){
        return etcdclient.getLeaseClient().keepAlive(leaseId, observer);
    }

    public void watch(String preKey, Consumer<WatchResponse> onNext, Consumer<Throwable> onError, Runnable runnable){
        ByteSequence bsKey = ByteSequence.from(preKey, Charsets.UTF_8);
        WatchOption wopt = WatchOption.newBuilder().withPrefix(bsKey).build();
        Watch.Watcher watcher = etcdclient.getWatchClient().watch(bsKey, wopt, onNext, onError, runnable);
    }

    @Override
    public void close() throws Exception {
        if(etcdclient != null){
            etcdclient.close();
            etcdclient = null;
        }
    }

    public void reset(){
        try{
            this.close();
        }catch (Throwable t){

        }
    }
}
