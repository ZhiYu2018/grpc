package com.gexiang.server;

import io.grpc.NameResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class GxNameResolverFactory extends NameResolver.Factory {
    private static Logger logger = LoggerFactory.getLogger(GxNameResolverFactory.class);
    @Override
    public String getDefaultScheme() {
        return "local";
    }

    @Override
    public NameResolver newNameResolver(URI targetUri, final NameResolver.Args args){
        return new GxNameResolver();
    }
}
