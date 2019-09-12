package com.gexiang.vo;

public class SessionInfo {
    public final static SessionInfo INVAL_SESSION = new SessionInfo(-1L, "N/A");
    private final long startTime;
    private final long sid;
    private final String remoteIp;
    public SessionInfo(long sid, String ip) {
        this.startTime = System.currentTimeMillis();
        this.sid = sid;
        this.remoteIp = ip;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getSid() {
        return sid;
    }

    public String getRemoteIp() {
        return remoteIp;
    }

}
