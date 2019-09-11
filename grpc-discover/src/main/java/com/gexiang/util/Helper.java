package com.gexiang.util;

import com.gexiang.core.EtcdData;
import com.google.common.base.Preconditions;
import javafx.util.Pair;

public class Helper {
    public static Pair<String, String> getServerKeyData(String key){
        /**prefix/package.server/ip/ver**/
        int index = key.indexOf("/");
        int lastIndex = key.lastIndexOf("/");
        if((index == -1) || (lastIndex == -1) || (index >= lastIndex)){
            throw new IllegalArgumentException("Could not extract service uri from " + key);
        }

        String url = key.substring(index + 1, lastIndex);
        index = ((String) Preconditions.checkNotNull(url, "url")).lastIndexOf(47);
        String fullServiceName = (index == -1 ? null : url.substring(0, index));
        if (fullServiceName == null) {
            throw new IllegalArgumentException("Could not extract full service from " + fullServiceName);
        }

        int serviceLength = fullServiceName.length();
        if (serviceLength + 1 >= url.length()
                || url.charAt(serviceLength) != '/') {
            throw new IllegalArgumentException("Could not extract ip from " + key);
        }

        String ip = url.substring(fullServiceName.length() + 1);
        return new Pair<>(fullServiceName, ip);
    }

    public static String createServerDataKey(String fullServerName, String url, String ver){
        return String.format("%s/%s/%s/%s", EtcdData.SERVER_PREFIX, fullServerName, url, ver);
    }

    public static String createServerCacheKey(String server, String ver){
        return String.format("%s/%s", server, ver);
    }
}
