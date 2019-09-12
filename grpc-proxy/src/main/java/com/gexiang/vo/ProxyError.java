package com.gexiang.vo;

public class ProxyError extends Throwable {
    public ProxyError(String message){
        super(message);
    }

    public ProxyError(int status, String msg){
        super(String.format("{\"status\":\"%d\",\"msg\":\"%s\"}", status, msg));
    }
}
