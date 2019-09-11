package com.gexiang.core;


public class GrpcFilter {
    private static class LazyHolder {
        static final GrpcFilter INSTANCE = new GrpcFilter();
    }

    public static GrpcFilter getInstance(){
        return LazyHolder.INSTANCE;
    }

    private GrpcFilter(){

    }


}
