package com.gexiang.core;

import com.gexiang.util.Helper;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.PutOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GrpcRegister implements AutoCloseable{
    private static Logger logger = LoggerFactory.getLogger(GrpcRegister.class);

    @Override
    public void close() throws Exception {
        if(etcdData != null) {
            etcdData.close();
            etcdData = null;
        }
    }

    private static class LazyHolder {
         static final GrpcRegister INSTANCE = new GrpcRegister();
    }

    private volatile EtcdData etcdData;
    private volatile LeaseGrantResponse leaseGrant;
    private volatile String localHost;
    private ScheduledExecutorService executorService;
    private volatile long lastKeepTime;

    public static GrpcRegister grpcRegister(){
        return LazyHolder.INSTANCE;
    }

    private GrpcRegister(){
        lastKeepTime = -1L;
    }

    public void init(Environment env){
        String url = env.getProperty(EtcdData.PROP_ETCD_HOST);
        logger.info("init etcd data url:{}", url);
        etcdData = new EtcdData(url);
        leaseGrant = etcdData.getLeaseId();
        if(leaseGrant == null){
            logger.error("Grant lease failed");
            return ;
        }

        localHost = getLocalHost();
        logger.info("Get leaseId {}, ttl:{} seconds", leaseGrant.getID(), leaseGrant.getTTL());
        /**创建keepalive:TTL is the server chosen lease time-to-live in seconds.**/
        long interval = (leaseGrant.getTTL()*1000) - 500L;
        executorService = Executors.newSingleThreadScheduledExecutor((r)->{ return new Thread(r,"etcd.keep.alive");});
        executorService.scheduleAtFixedRate(()->{ GrpcRegister.this.keepAlive(); }, 0, interval, TimeUnit.MILLISECONDS);
    }

    private void keepAlive(){
        if((lastKeepTime > 0) && ((System.currentTimeMillis() - lastKeepTime)) >= leaseGrant.getTTL()*1000L){
            logger.error("Keep alieve time out:{}", (System.currentTimeMillis() - lastKeepTime));
        }
        lastKeepTime = System.currentTimeMillis();
        etcdData.keepLeaseIdAlive(leaseGrant.getID());
    }

    private String getLocalHost(){
        return "127.0.0.1";
    }

    public void register(String fullServerName, String port, String ver){
        if(leaseGrant == null){
            logger.error("Can not register server:{}", fullServerName);
            return;
        }

        String key = Helper.createServerDataKey(fullServerName, String.format("%s:%s", localHost, port), ver);
        logger.info("Register {} with id {}", key, leaseGrant.getID());
        etcdData.put(key, String.valueOf(System.currentTimeMillis()/1000),
                     PutOption.newBuilder().withLeaseId(leaseGrant.getID()).build());
    }
}
