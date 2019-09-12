package com.gexiang.core;

import com.gexiang.util.GrpcBackOff;
import com.gexiang.util.Helper;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.PutOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import java.net.Inet4Address;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
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
    private volatile ConcurrentHashMap<String, String> appServer;
    private ScheduledExecutorService executorService;
    private volatile long lastKeepTime;

    public static GrpcRegister grpcRegister(){
        return LazyHolder.INSTANCE;
    }

    private GrpcRegister(){
        lastKeepTime = -1L;
        appServer = new ConcurrentHashMap<>();
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
        long interval = 150L;
        /**这个要注意优先级，在压测的情况下，出现keepalive 假死**/
        executorService = Executors.newSingleThreadScheduledExecutor((r)->{ return new Thread(r,"etcd.keep.alive");});
        executorService.scheduleAtFixedRate(()->{ GrpcRegister.this.keepAlive(); }, 0, interval, TimeUnit.MILLISECONDS);
    }

    private void keepAlive(){
        if(((lastKeepTime > 0) && ((System.currentTimeMillis() - lastKeepTime)) >= leaseGrant.getTTL()*1000L)
           || (leaseGrant == null)){
            /**兜底**/
            logger.error("Keep alieve time out:{}", (System.currentTimeMillis() - lastKeepTime));
            if(doGetLeaseId() == 0) {
                if(doRegister() == 0) {
                    lastKeepTime = System.currentTimeMillis();
                }
            }
            return ;
        }

        lastKeepTime = System.currentTimeMillis();
        GrpcBackOff grpcBackOff = new GrpcBackOff();
        while (true) {
            if(etcdData.keepLeaseIdAlive(leaseGrant.getID()) == 0){
                break;
            }

            long nextMills = grpcBackOff.nextBackOffMillis();
            if(nextMills == GrpcBackOff.STOP){
                /**连接可能断掉，需要重新连接，获取租期**/
                leaseGrant = null;
                break;
            }else{
                Helper.sleep(nextMills);
            }
        }
    }

    private String getLocalHost(){
        Optional<Inet4Address> opt = Helper.getLocalIp4Address();
        if(opt.get() == null){
            logger.error("Get local inner ip failed");
            return null;
        }

        /**\/192.168.1.5**/
        return opt.get().getHostAddress();

    }

    private int doGetLeaseId(){
        int times = 0;
        GrpcBackOff grpcBackOff = new GrpcBackOff();
        while (true){
            /**重新连接，获取租期**/
            times ++;
            if(leaseGrant == null){
                leaseGrant = etcdData.reconnect();
            }else {
                leaseGrant = etcdData.getLeaseId();
                if(times >= 3) {
                    /**重新连接**/
                    leaseGrant = null;
                }
            }
            if(leaseGrant != null){
                return 0;
            }

            long nextMills = grpcBackOff.nextBackOffMillis();
            if(nextMills == GrpcBackOff.STOP){
                logger.error("Retry times over");
                return -1;
            }
            Helper.sleep(nextMills);
        }
    }

    private int doRegister(){
        PutOption putOption = PutOption.newBuilder().withLeaseId(leaseGrant.getID()).build();
        synchronized (this){
            for(Map.Entry<String, String> entry: appServer.entrySet()){
                int index = entry.getKey().lastIndexOf(":");
                String fullServerName = entry.getKey().substring(0, index);
                String port = entry.getKey().substring(index + 1);
                String key = Helper.createServerDataKey(fullServerName, String.format("%s:%s", localHost, port),
                                                        entry.getValue());
                if(etcdData.put(key, String.valueOf(System.currentTimeMillis()/1000), putOption) != 0){
                    return -1;
                }
            }
        }

        return 0;
    }

    public void register(String fullServerName, String port, String ver){
        if(leaseGrant == null){
            logger.error("Can not register server:{}", fullServerName);
            return;
        }

        if(localHost == null){
            logger.error("Can not get local ip for:{}", fullServerName);
            return;
        }

        String hkey = String.format("%s:%s", fullServerName, port);
        if(appServer.containsKey(hkey)){
            return ;
        }

        String key = Helper.createServerDataKey(fullServerName, String.format("%s:%s", localHost, port), ver);
        logger.info("Register {} with id {}", key, leaseGrant.getID());
        etcdData.put(key, String.valueOf(System.currentTimeMillis()/1000),
                     PutOption.newBuilder().withLeaseId(leaseGrant.getID()).build());
        synchronized (this){
            appServer.put(hkey, ver);
        }
    }
}
