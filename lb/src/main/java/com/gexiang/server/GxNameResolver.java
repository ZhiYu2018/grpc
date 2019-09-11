package com.gexiang.server;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;

public class GxNameResolver extends NameResolver {
    private static Logger logger = LoggerFactory.getLogger(GxNameResolver.class);
    private NameResolver.Listener2 listener;

    @Override
    public String getServiceAuthority() {
        return "none";
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void start(NameResolver.Listener2 listener) {
        logger.info("start ......");
        this.listener = listener;
        List<SocketAddress> addrs = new ArrayList<>();
        addrs.add(new InetSocketAddress("127.0.0.1", 8300));
        addrs.add(new InetSocketAddress("127.0.0.1", 8301));
        addrs.add(new InetSocketAddress("127.0.0.1", 8303));
        List<EquivalentAddressGroup> addressGroups = new ArrayList<EquivalentAddressGroup>();
        addressGroups.add(new EquivalentAddressGroup(addrs));
        this.listener.onResult(NameResolver.ResolutionResult.newBuilder().setAddresses(addressGroups).build());
    }
}
