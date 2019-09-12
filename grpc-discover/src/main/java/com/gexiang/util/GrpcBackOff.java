package com.gexiang.util;

public class GrpcBackOff {
    public static final long STOP = -1L;
    private static final int DEFAULT_INITIAL_INTERVAL_MILLIS = 500;
    private static final double DEFAULT_MULTIPLIER = 1.5D;
    private static final int DEFAULT_MAX_INTERVAL_MILLIS = 60000;
    private static final int DEFAULT_MAX_ELAPSED_TIME_MILLIS = 900000;
    private static double	DEFAULT_RANDOMIZATION_FACTOR = 0.5D;
    private long interval;
    private long maxInterval;
    private long maxElapsedTime;
    private double randomization_factor;
    private double multiplier;
    private long startTime;
    private long retry_interval;

    public GrpcBackOff(){
        interval = DEFAULT_INITIAL_INTERVAL_MILLIS;
        maxInterval = DEFAULT_MAX_INTERVAL_MILLIS;
        maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME_MILLIS;
        randomization_factor = DEFAULT_RANDOMIZATION_FACTOR;
        multiplier = DEFAULT_MULTIPLIER;
        startTime = System.currentTimeMillis();
        retry_interval = 0;
    }

    public long	nextBackOffMillis(){
        if((System.currentTimeMillis() - startTime) >= maxElapsedTime){
            return STOP;
        }

        if(retry_interval == 0){
            retry_interval = interval;
        }else{
            if(retry_interval < maxInterval) {
                retry_interval = (long) (retry_interval * multiplier);
            }
        }
        double min = 1 - randomization_factor;
        double max = 1 + randomization_factor;
        double rf  = min + Math.random()*(max - min);
        long  randomized_interval = (long)(retry_interval * rf);
        return randomized_interval;
    }

}
