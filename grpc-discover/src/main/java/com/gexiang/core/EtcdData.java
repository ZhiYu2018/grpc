package com.gexiang.core;

import com.google.common.base.Charsets;
import io.etcd.jetcd.*;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.lease.LeaseRevokeResponse;
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

    private static final int MAX_GET_LIMIT = 1000;
    private static final Logger logger = LoggerFactory.getLogger(EtcdData.class);
    private volatile Client etcdclient;
    public EtcdData(String hostList){
        logger.info("Etcd host is:{}", hostList);
        etcdclient = Client.builder().endpoints(hostList.split(";")).build();
    }


    public void put(String key, String value){
        ByteSequence bsKey = ByteSequence.from(key, Charsets.UTF_8);
        ByteSequence bsValue = ByteSequence.from(value, Charsets.UTF_8);
        CompletableFuture<PutResponse> future = etcdclient.getKVClient().put(bsKey, bsValue);
        try {
            PutResponse rsp = future.get();
            logger.info("Get rsp:{}", rsp.getPrevKv().getVersion());
        }catch (Throwable t){
            logger.warn("Put key {} exceptions:", key, t);
        }
    }

    public void put(String key, String value, PutOption option){
        ByteSequence bsKey = ByteSequence.from(key, Charsets.UTF_8);
        ByteSequence bsValue = ByteSequence.from(value, Charsets.UTF_8);
        CompletableFuture<PutResponse> future = etcdclient.getKVClient().put(bsKey, bsValue, option);
        try {
            PutResponse rsp = future.get();
        }catch (Throwable t){
            if(t instanceof StatusRuntimeException){
                StatusRuntimeException sre = (StatusRuntimeException)t;
                logger.error("Put key {} failed:{}", key, sre.getStatus().getCode().name());
                return;
            }
            logger.warn("Put key {} exceptions:", key, t);
        }
    }

    public String get(String key){
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
            logger.warn("Get key {} exceptions:", key, t);
            return null;
        }
    }

    public List<KeyValue> listPreKey(String preKey){

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
            logger.warn("Get key {} exceptions:", preKey, t);
            return null;
        }
    }


    public LeaseGrantResponse getLeaseId(){
        CompletableFuture<LeaseGrantResponse> future = etcdclient.getLeaseClient().grant(0L);
        try{
            LeaseGrantResponse rsp = future.get(3, TimeUnit.SECONDS);
            logger.info("grant lease id {}, ttl {}", rsp.getID(), rsp.getTTL());
            return rsp;
        }catch (Throwable t){
            logger.warn("grant lease id exceptions:", t);
        }

        return null;
    }

    public void keepLeaseIdAlive(long leaseId){
        CompletableFuture<LeaseKeepAliveResponse> future = etcdclient.getLeaseClient().keepAliveOnce(leaseId);
        try{
            LeaseKeepAliveResponse rsp = future.get();
        }catch (Throwable t){
            logger.warn("keep avlie lease id {} exceptions:", leaseId, t);
        }
    }

    public int revoke(long leaseId){
        CompletableFuture<LeaseRevokeResponse> future = etcdclient.getLeaseClient().revoke(leaseId);
        try{
            LeaseRevokeResponse lrsp = future.get();
            logger.info("Revoke msg:{}", lrsp.toString());
            return 0;
        }catch (Throwable t){
            logger.warn("Revoke leaseId {}, exceptions {}", leaseId, t.getMessage());
        }
        return -1;
    }
    
    public void watch(String preKey, Consumer<WatchResponse> onNext, Consumer<Throwable> onError){
        ByteSequence bsKey = ByteSequence.from(preKey, Charsets.UTF_8);
        WatchOption wopt = WatchOption.newBuilder().withPrefix(bsKey).build();
        Watch.Watcher watcher = etcdclient.getWatchClient().watch(bsKey,wopt, onNext, onError);
    }

    @Override
    public void close() throws Exception {
        if(etcdclient != null){
            etcdclient.close();
            etcdclient = null;
        }
    }
}
