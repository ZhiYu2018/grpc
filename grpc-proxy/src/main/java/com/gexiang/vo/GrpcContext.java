package com.gexiang.vo;

public class GrpcContext {
    private final String method;
    private final String ver;
    private final String body;
    private final SessionInfo sessionInfo;

    public GrpcContext(String method, String ver, String body, SessionInfo sessionInfo) {
        this.method = method;
        this.ver = ver;
        this.body = body;
        this.sessionInfo = sessionInfo;
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public static class Builder{
        private  String method;
        private  String ver;
        private  String body;
        private  SessionInfo sessionInfo;
        private Builder(){
        }

        public Builder setMethod(String method) {
            this.method = method;
            return this;
        }

        public Builder setVer(String ver) {
            this.ver = ver;
            return this;
        }

        public Builder setBody(String body) {
            this.body = body;
            return this;
        }

        public Builder setSessionInfo(SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
            return this;
        }
        public GrpcContext build(){
            return new GrpcContext(method, ver, body, sessionInfo);
        }
    }

    public String getMethod() {
        return method;
    }

    public String getVer() {
        return ver;
    }

    public String getBody() {
        return body;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }
}
