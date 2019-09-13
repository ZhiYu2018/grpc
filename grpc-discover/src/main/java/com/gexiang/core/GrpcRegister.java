package com.gexiang.core;

import com.gexiang.core.vo.KeepStatus;
import com.gexiang.util.GrpcBackOff;
import com.gexiang.util.Helper;
import io.etcd.jetcd.CloseableClient;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
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
    private volatile CloseableClient observerClient;
    private volatile KeepStatus keepStatus;
    private ScheduledExecutorService executorService;
    private volatile long lastKeepTime;

    public static GrpcRegister grpcRegister(){
        return LazyHolder.INSTANCE;
    }

    private GrpcRegister(){
        lastKeepTime = -1L;
        appServer = new ConcurrentHashMap<>();
        keepStatus = KeepStatus.KEEP_IDLE;
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
        logger.info("{} get leaseId {}, ttl:{} seconds", localHost, leaseGrant.getID(), leaseGrant.getTTL());
        /**创建keepalive:TTL is the server chosen lease time-to-live in seconds.**/
        /**记录keep alive**/
        observerClient = etcdData.keepLeaseObserver(leaseGrant.getID(), new EtcdKeepObserver(this::handleObserver));
        lastKeepTime = System.currentTimeMillis();
        keepStatus = KeepStatus.KEEP_SUCCESS;
        executorService = Executors.newSingleThreadScheduledExecutor((r)->{ return new Thread(r,"etcd.keep.alive");});
        executorService.scheduleAtFixedRate(()->{ GrpcRegister.this.keepStatusMachine(); }, 0, 1, TimeUnit.SECONDS);
    }

    private void handleObserver(Object object){
        if(etcdData == null){
            logger.warn("Out of exceptions");
            return ;
        }
        if(object instanceof LeaseKeepAliveResponse){
            LeaseKeepAliveResponse response = (LeaseKeepAliveResponse)object;
            long timeUsed = (System.currentTimeMillis() - lastKeepTime);
            if(timeUsed>= leaseGrant.getTTL()*1000L) {
                logger.warn("Keep alive time out:{},{}, time used:{}", response.getID(), response.getTTL(), timeUsed);
                keepStatus = KeepStatus.KEEP_TIME_OUT;
            }

            lastKeepTime = System.currentTimeMillis();
        }else if(object instanceof Throwable){
            logger.error("Keep error:{}", ((Throwable) object).getMessage());
            keepStatus = KeepStatus.KEEP_ERROR;
        }else {
            logger.info("Keep alive:{}", object);
            /**要重新连接**/
            keepStatus = KeepStatus.KEEP_ERROR;
        }
    }

    private void keepStatusMachine(){
        if(keepStatus == KeepStatus.KEEP_TIME_OUT){
            /**先重新获取租期**/
            logger.info("Handle time out ......");
            if(doGetLeaseId() == 0){
                lastKeepTime = System.currentTimeMillis();
                if(doRegister() == 0){
                    logger.info("Time out [Do register success]");
                    keepStatus = KeepStatus.KEEP_SUCCESS;
                }else{
                    /**重新注册**/
                    keepStatus = KeepStatus.KEEP_REGISTER;
                    logger.info("Time out [Do register failed]");
                }
            }
            return ;
        }else if(keepStatus == KeepStatus.KEEP_ERROR) {
            logger.info("Handle reconnect ......");
            if (observerClient != null) {
                observerClient.close();
            }
            leaseGrant = etcdData.reconnect();
            if (leaseGrant != null) {
                logger.info("Get leaseid {}, ttl {}", leaseGrant.getID(), leaseGrant.getTTL());
                observerClient = etcdData.keepLeaseObserver(leaseGrant.getID(), new EtcdKeepObserver(this::handleObserver));
                lastKeepTime = System.currentTimeMillis();
                if (doRegister() == 0) {
                    logger.info("Reconnect [Do reconnect success]");
                    keepStatus = KeepStatus.KEEP_SUCCESS;
                } else {
                    /**重新注册**/
                    logger.info("Reconnect [Do reconnect failed]");
                    keepStatus = KeepStatus.KEEP_REGISTER;
                }
            }
        }else if(keepStatus == KeepStatus.KEEP_REGISTER){
            logger.info("Handle register ......");
            if(doRegister() == 0){
                logger.info("Do register success");
                keepStatus = KeepStatus.KEEP_SUCCESS;
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
            leaseGrant = etcdData.getLeaseId();
            if(times >= 3) {
                /**重新连接**/
                logger.info("Try to reconnect");
                leaseGrant = null;
            }
            if(leaseGrant != null){
                logger.info("Do get leaseId success");
                return 0;
            }

            long nextMills = grpcBackOff.nextBackOffMillis();
            if(nextMills == GrpcBackOff.STOP){
                logger.error("Do get leaseId retry times over");
                keepStatus = KeepStatus.KEEP_ERROR;
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
                logger.info("Register {} with id {} again", key, leaseGrant.getID());
                if(etcdData.put(key, String.valueOf(System.currentTimeMillis()/1000), putOption) != 0){
                    logger.error("Do register failed");
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
