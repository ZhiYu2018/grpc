package com.gexiang.vo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class StepValue {
    private final static long INTERVAL_SECONDS = 5*1000L;
    private AtomicReference<Long> startTime;
    private AtomicInteger currentNum;
    public StepValue(){
        startTime = new AtomicReference<>(Long.valueOf(System.currentTimeMillis()));
        currentNum = new AtomicInteger(0);
    }

    public int incAndGet(){
        int num = currentNum.get();
        Long v  = startTime.get();
        if((System.currentTimeMillis() - v.longValue()) >= INTERVAL_SECONDS){
            if(startTime.compareAndSet(v, Long.valueOf(System.currentTimeMillis()))) {
                currentNum.set(currentNum.get() - num);
            }
        }

        return currentNum.incrementAndGet();
    }
}
