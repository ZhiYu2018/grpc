package com.gexiang.vo;

public class ProxyResponse {
    private final int code;
    private final String msg;

    public ProxyResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public String toJson(){
        return String.format("{\"code\":\"%d\",\"msg\":\"%s\"}", code, msg);
    }
}
