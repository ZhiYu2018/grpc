package com.gexiang.core;


import com.gexiang.vo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Mono;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class GrpcConManger {
    private static final long HOUR_SECONDS = 3600L;
    private static final int IP_FAST_LEVEL = 1000;
    private static final int IP_MIDDLE_LEVEL = 1500;
    private static final int IP_SLOWER_LEVEL = 2500;
    private static final int FAST_RATE = 250;
    private static final int MIDDLE_RATE = 100;
    private static final int SLOW_RATE = 50;
    private static final int MAX_QUEUE_SIZE = 10000;

    private static Logger logger = LoggerFactory.getLogger("grpc.con.mgr");
    private static class LazyHolder {
        static final GrpcConManger INSTANCE = new GrpcConManger();
    }

    public static GrpcConManger getInstance(){
        return LazyHolder.INSTANCE;
    }

    private volatile long cacheTime;
    private ConcurrentHashMap<String, StepValue> ipValue;
    private AtomicLong requestTimes;
    private AtomicLong getRequestTimeUesed;
    private ThreadLocal<SessionInfo> httpNioThreadLocal;
    private GrpcWorker fastWorker;
    private GrpcWorker middleWorker;
    private GrpcWorker slowWorker;

    private GrpcConManger(){
        cacheTime = System.currentTimeMillis();
        ipValue    = new ConcurrentHashMap<>();
        requestTimes = new AtomicLong(0);
        getRequestTimeUesed = new AtomicLong(0);
        httpNioThreadLocal = new ThreadLocal<>();
        int cpuNum = Runtime.getRuntime().availableProcessors();
        logger.info("Cpu num:{}", cpuNum);
        fastWorker = new GrpcWorker(cpuNum, FAST_RATE, MAX_QUEUE_SIZE, "fast.worker");
        middleWorker = new GrpcWorker(cpuNum, MIDDLE_RATE, MAX_QUEUE_SIZE, "middle.worker");
        slowWorker = new GrpcWorker(cpuNum, SLOW_RATE, MAX_QUEUE_SIZE, "slow.worker");
    }

    public void stop(){
        fastWorker.setStop();
        middleWorker.setStop();
        slowWorker.setStop();
    }

    public void init(Environment env){

    }

    public void addSession(SessionInfo sessionInfo){
        httpNioThreadLocal.set(sessionInfo);
    }

    public Mono<String> limitRequest(GrpcContext grpcContext){
        /**判断ip是否太频繁**/
        int q = getIpConcurrent(grpcContext.getSessionInfo().getRemoteIp());
        if(q < IP_FAST_LEVEL){
            return Mono.create(new GrpcConsumer(grpcContext, fastWorker));
        }else if(q < IP_MIDDLE_LEVEL){
            return Mono.create(new GrpcConsumer(grpcContext, middleWorker));
        }else if(q < IP_SLOWER_LEVEL){
            return Mono.create(new GrpcConsumer(grpcContext, slowWorker));
        }

        int value = HttpStatus.TOO_MANY_REQUESTS.value();
        String msg = HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase();
        return Mono.error(new ProxyError(value, msg));

    }

    public SessionInfo removeSession(){
        SessionInfo sessionInfo = httpNioThreadLocal.get();
        httpNioThreadLocal.remove();
        if(sessionInfo == null){
            sessionInfo = SessionInfo.INVAL_SESSION;
        }
        return sessionInfo;
    }

    public void reqPerf(GrpcContext grpcContext){
        long times = requestTimes.incrementAndGet();
        long mills = System.currentTimeMillis() - grpcContext.getSessionInfo().getStartTime();
        long timeUsed = getRequestTimeUesed.addAndGet(mills);
        if(times == 500){
            logger.info("Mean perf time used:{}", timeUsed/times);
            requestTimes.set((requestTimes.get()) % 500);
            getRequestTimeUesed.set(getRequestTimeUesed.get() - timeUsed);
        }
        logger.debug("Session id:{} from ip:{} method:{} ver:{} time used:{}", grpcContext.getSessionInfo().getSid(),
                   grpcContext.getSessionInfo().getRemoteIp(), grpcContext.getMethod(),
                   grpcContext.getVer(), mills);
    }

    private int getIpConcurrent(String ip){
        if((ip == null) || ConstValues.IG_IP.equals(ip)){
            return 0;
        }

        StepValue stepValue = ipValue.get(ip);
        if(stepValue == null){
            synchronized (this){
                if(((System.currentTimeMillis() - cacheTime)/1000) >= HOUR_SECONDS){
                    /***每一小时清除一次信息**/
                    ipValue.clear();
                    cacheTime = System.currentTimeMillis();
                }
                stepValue = ipValue.get(ip);
                if(stepValue == null){
                    stepValue = new StepValue();
                    ipValue.put(ip, stepValue);
                }
            }
        }

        return stepValue.incAndGet();
    }


}
