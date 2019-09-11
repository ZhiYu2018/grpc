package com.gexiang.core.vo;

public class ProtoServer {
    private final String packageName;
    private final String serviceName;
    public ProtoServer(String packageName, String serviceName){
       this.packageName = packageName;
       this.serviceName = serviceName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public static ProtoServer create(String fullServiceName){
        int index = fullServiceName.lastIndexOf(".");
        if (index == -1) {
            throw new IllegalArgumentException("Could not extract package name from " + fullServiceName);
        }

        String packageName = fullServiceName.substring(0, index);
        // Make sure there is a '.' and use the rest as the service name.
        if (index + 1 >= fullServiceName.length() || fullServiceName.charAt(index) != '.') {
            throw new IllegalArgumentException("Could not extract service from " + fullServiceName);
        }
        String serviceName = fullServiceName.substring(index + 1);
        return new ProtoServer(packageName, serviceName);
    }
}
