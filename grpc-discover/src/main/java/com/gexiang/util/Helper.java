package com.gexiang.util;

import com.gexiang.core.EtcdData;
import com.google.common.base.Preconditions;
import javafx.util.Pair;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

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

    public static String getLocalInnerIp(){
        return "";
    }

    private static Optional<Inet4Address> getIpBySocket(){
        try (final DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            if (socket.getLocalAddress() instanceof Inet4Address) {
                return Optional.of((Inet4Address) socket.getLocalAddress());
            }
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }catch (SocketException e){

        }
        return Optional.empty();
    }

    private static List<Inet4Address> getIpByNetworkIf() throws SocketException{
        List<Inet4Address> addresses = new ArrayList<>(1);
        Enumeration e = NetworkInterface.getNetworkInterfaces();
        if (e == null) {
            return addresses;
        }
        while (e.hasMoreElements()) {
            NetworkInterface n = (NetworkInterface) e.nextElement();
            if (!isValidInterface(n)) {
                continue;
            }
            Enumeration ee = n.getInetAddresses();
            while (ee.hasMoreElements()) {
                InetAddress i = (InetAddress) ee.nextElement();
                if (isValidAddress(i)) {
                    addresses.add((Inet4Address) i);
                }
            }
        }

        return addresses;
    }

    private static boolean isValidInterface(NetworkInterface ni) throws SocketException {
        return !ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual()
                && (ni.getName().startsWith("eth") || ni.getName().startsWith("ens"));
    }

    private static boolean isValidAddress(InetAddress address) {
        return address instanceof Inet4Address && address.isSiteLocalAddress() && !address.isLoopbackAddress();
    }

    public static Optional<Inet4Address> getLocalIp4Address(){
        try {
            final List<Inet4Address> ipByNi = getIpByNetworkIf();
            if (ipByNi.isEmpty() || ipByNi.size() > 1) {
                final Optional<Inet4Address> ipBySocketOpt = getIpBySocket();
                if (ipBySocketOpt.isPresent()) {
                    return ipBySocketOpt;
                } else {
                    return ipByNi.isEmpty() ? Optional.empty() : Optional.of(ipByNi.get(0));
                }
            }
            return Optional.of(ipByNi.get(0));
        }catch (Throwable t){

        }
        return Optional.empty();
    }

}
