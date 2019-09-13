package com.gexiang.core;

import com.gexiang.core.vo.GrpcConnector;
import com.gexiang.util.Helper;
import com.google.common.base.Charsets;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.watch.WatchEvent;
import io.etcd.jetcd.watch.WatchResponse;
import io.grpc.ManagedChannel;
import javafx.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;


public class GrpcChannelPool implements AutoCloseable{
    private static class LazyHolder {
        static final GrpcChannelPool INSTANCE = new GrpcChannelPool();
    }
    private static Logger logger = LoggerFactory.getLogger(GrpcChannelPool.class);
    private AtomicReference<ConcurrentHashMap<String, GrpcConnector>> grpcPoolRef;
    private EtcdData etcdData;

    public static GrpcChannelPool getPool(){
        return LazyHolder.INSTANCE;
    }

    @Override
    public void close() throws Exception {
        ConcurrentHashMap<String, GrpcConnector> pool = grpcPoolRef.getAndSet(null);
        if(pool != null){
            for(GrpcConnector c: pool.values()){
                try{
                    c.close();
                }catch (Throwable t){

                }
            }
            pool.clear();
        }
        if(etcdData != null) {
            etcdData.close();
            etcdData = null;
        }
    }

    class EtcdWaterConsumer implements Consumer<WatchResponse>{
        @Override
        public void accept(WatchResponse response){
            List<WatchEvent> events = response.getEvents();
            logger.info("Consume {} events", events.size());
            for(WatchEvent we: events){
                String key = we.getKeyValue().getKey().toString(Charsets.UTF_8);
                try{
                    Pair<String, String> kp = Helper.getServerKeyData(key);
                    String ver = null;
                    int idx = key.lastIndexOf("/");
                    if((idx >= 0 && idx < key.length())){
                        ver = key.substring(idx + 1);
                    }

                    switch (we.getEventType()){
                        case PUT:
                            GrpcChannelPool.this.addServer(kp, ver);
                            break;
                        case DELETE:
                            GrpcChannelPool.this.kickOut(kp, ver);
                            break;
                        case UNRECOGNIZED:
                            logger.info("{} is unknow event", key);
                            break;
                    }
                }catch (Throwable t){
                    logger.info("Parse key {} exceptions:", key, t);
                }
            }
        }
    }

    class EtcdErrorConsumer implements Consumer<Throwable>{
        @Override
        public void accept(Throwable t){
            logger.warn("Watch exceptions:{}", t.getMessage());
        }
    }

    private GrpcChannelPool(){

    }

    public void init(Environment env){
        grpcPoolRef = new AtomicReference<>();
        grpcPoolRef.set(new ConcurrentHashMap<>());
        String host = env.getProperty(EtcdData.PROP_ETCD_HOST);
        logger.info("Get etcd host:{}", host);
        etcdData = new EtcdData(host);
        loadGrpcInfo();
        /****/
        addWatch();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC proxy since JVM is shutting down");
                logger.info("*** shutting down gRPC proxy since JVM is shutting down");
                try{
                    GrpcChannelPool.this.close();
                    etcdData.close();
                }catch (Throwable t){

                }
                System.err.println("*** server shut down");
                logger.info("*** server shut down");
            }
        });
        logger.info("Init ok");
    }

    public ManagedChannel getChannel(String fullServerName, String ver){
        ConcurrentHashMap<String, GrpcConnector> pool = grpcPoolRef.get();
        if(pool == null){
            return null;
        }

        String serverKey = Helper.createServerCacheKey(fullServerName, ver);
        GrpcConnector connector = pool.get(serverKey);
        if(connector == null){
            logger.info("Find none such connector for {}", serverKey);
            return null;
        }

        return connector.getChannel();
    }

    private void loadGrpcInfo(){
        List<KeyValue> kvs = etcdData.listPreKey(EtcdData.SERVER_PREFIX);
        logger.info("Get keys:{}", kvs.size());
        for(KeyValue kv:kvs){
            /**package.server/ip**/
            String key = kv.getKey().toString(Charsets.UTF_8);
            String ver = null;
            int idx = key.lastIndexOf("/");
            if((idx >= 0 && idx < key.length())){
                ver = key.substring(idx + 1);
            }
            try{
                Pair<String, String> kp = Helper.getServerKeyData(key);
                String serverKey = Helper.createServerCacheKey(kp.getKey(), ver);
                GrpcConnector connector = grpcPoolRef.get().get(serverKey);
                if(connector == null){
                    connector = new GrpcConnector();
                    grpcPoolRef.get().put(serverKey, connector);
                }

                connector.addAddress(kp.getValue());
            }catch (Throwable t){
                logger.warn("Parse key {}, exceptions:", key, t);
            }
        }
    }

    private void kickOut(Pair<String,String> kp, String ver){
        ConcurrentHashMap<String, GrpcConnector> pool = grpcPoolRef.get();
        if(pool == null){
            return;
        }

        logger.info("Kick out {}/{}.{}", kp.getKey(), kp.getValue(), ver);
        String serverKey = Helper.createServerCacheKey(kp.getKey(), ver);
        GrpcConnector connector = pool.get(serverKey);
        if(connector != null){
            connector.kickOutChannel(kp.getKey(), kp.getValue());
            if(connector.getAddrsCount() == 0){
                pool.remove(serverKey);
            }
        }
    }

    private void addServer(Pair<String,String> kp, String ver){
        ConcurrentHashMap<String, GrpcConnector> pool = grpcPoolRef.get();
        if(pool == null){
            return;
        }

        logger.info("Add server {}/{}.{}", kp.getKey(), kp.getValue(), ver);
        String serverKey = Helper.createServerCacheKey(kp.getKey(), ver);
        GrpcConnector connector = pool.get(serverKey);
        if(connector == null){
            /**并发**/
            connector = new GrpcConnector();
            connector.addAddress(kp.getValue());
            pool.put(serverKey, connector);
        }else{
            connector.addAddress(kp.getValue());
        }
    }

    private void addWatch(){
        etcdData.watch(EtcdData.SERVER_PREFIX, new EtcdWaterConsumer(), new EtcdErrorConsumer(), new Runnable(){
            @Override
            public void run() {
                logger.warn("Watch complete");
            }
        });
    }
}
