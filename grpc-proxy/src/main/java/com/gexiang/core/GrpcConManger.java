package com.gexiang.core;


import com.gexiang.vo.GrpcContext;
import com.gexiang.vo.SessionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicLong;

public class GrpcConManger {
    private static Logger logger = LoggerFactory.getLogger("grpc.con.mgr");
    private static class LazyHolder {
        static final GrpcConManger INSTANCE = new GrpcConManger();
    }

    public static GrpcConManger getInstance(){
        return LazyHolder.INSTANCE;
    }

    private ThreadLocal<SessionInfo> httpNioThreadLocal;
    private AtomicLong requestTimes;
    private AtomicLong getRequestTimeUesed;

    private GrpcConManger(){
        httpNioThreadLocal = new ThreadLocal<>();
        requestTimes = new AtomicLong(0);
        getRequestTimeUesed = new AtomicLong(0);
    }

    public void init(Environment env){

    }

    public Mono<String> validate(){
        return null;
    }

    public void addSession(SessionInfo sessionInfo){
        httpNioThreadLocal.set(sessionInfo);
    }

    public boolean limitRequest(GrpcContext grpcContext){
        /**判断ip是否太频繁**/
        return false;
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
        long times = requestTimes.getAndIncrement();
        long mills = System.currentTimeMillis() - grpcContext.getSessionInfo().getStartTime();
        long timeUsed = getRequestTimeUesed.getAndAdd(mills);
        if(times >= 500){
            logger.info("Mean perf time used:{}", timeUsed/times);
            requestTimes.set(0);
            getRequestTimeUesed.set(0);
        }
        logger.debug("Session id:{} from ip:{} method:{} ver:{} time used:{}", grpcContext.getSessionInfo().getSid(),
                   grpcContext.getSessionInfo().getRemoteIp(), grpcContext.getMethod(),
                   grpcContext.getVer(), mills);

    }


}
