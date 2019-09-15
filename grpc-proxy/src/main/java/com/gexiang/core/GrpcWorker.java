package com.gexiang.core;

import com.google.common.util.concurrent.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class GrpcWorker {
    private static Logger logger = LoggerFactory.getLogger(GrpcWorker.class);
    private volatile boolean stop;
    private AtomicInteger queNum;
    private int maxQueSize;
    private ConcurrentLinkedDeque<GrpcConsumer> grpcConsumersList;
    private RateLimiter rateLimiter;
    private Semaphore semaphore;
    private List<Thread> threads;
    public GrpcWorker(int num, int limiter, int maxQueSize, String threadName){
        stop = false;
        queNum = new AtomicInteger(0);
        this.maxQueSize = maxQueSize;
        grpcConsumersList = new ConcurrentLinkedDeque<>();
        rateLimiter = RateLimiter.create(limiter);
        semaphore  = new Semaphore(0);
        threads   = new ArrayList<>();
        for(int n = 0; n < num; n++){
            Thread th = new Thread(()->{ GrpcWorker.this.work(); }, String.format("%s.%d", threadName, n));
            th.start();
            threads.add(th);
        }

        logger.info("Thread {} init ok", threadName);
    }

    public void setStop(){
        stop = true;
    }
    public int push(GrpcConsumer grpcConsumer){
        /**增加队列数**/
        int qn = queNum.decrementAndGet();
        if(qn > maxQueSize){
            queNum.decrementAndGet();
            logger.warn("Queue size {} is to many!!!", qn);
            return -1;
        }

        grpcConsumersList.offerLast(grpcConsumer);
        semaphore.release();
        return 0;
    }


    private void work(){
        logger.info("{} is running ......", Thread.currentThread().getName());
        while (!stop){
            try {
                if (semaphore.tryAcquire(3, TimeUnit.SECONDS)) {
                    /**限速，执行**/
                    rateLimiter.acquire();
                    GrpcConsumer grpcConsumer = grpcConsumersList.pollFirst();
                    if(grpcConsumer == null){
                        continue;
                    }

                    queNum.decrementAndGet();
                    grpcConsumer.forward();
                }
            }catch (Throwable t){
                logger.warn("Forward work:{}", t.getMessage());
            }
        }
    }
}
