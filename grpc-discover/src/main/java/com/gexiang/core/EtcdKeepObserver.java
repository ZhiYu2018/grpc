package com.gexiang.core;

import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class EtcdKeepObserver implements StreamObserver<LeaseKeepAliveResponse> {
    private static Logger logger = LoggerFactory.getLogger(EtcdKeepObserver.class);
    private final Consumer<Object> consumer;
    public EtcdKeepObserver(Consumer<Object> func){
        this.consumer = func;
    }
    @Override
    public void onNext(LeaseKeepAliveResponse leaseKeepAliveResponse) {
        consumer.accept(leaseKeepAliveResponse);
    }

    @Override
    public void onError(Throwable throwable) {
        consumer.accept(throwable);
    }

    @Override
    public void onCompleted() {
        consumer.accept("Completed");
    }
}
