package com.gexiang.core;

import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonMessageHandler {
    private static Logger logger = LoggerFactory.getLogger(JsonMessageHandler.class);
    private final JsonFormat.Parser jsonParser;
    private final Descriptors.Descriptor descriptor;
    public JsonMessageHandler(JsonFormat.TypeRegistry registry, Descriptors.Descriptor descriptor){
        this.jsonParser = JsonFormat.parser().usingTypeRegistry(registry);
        this.descriptor = descriptor;
    }


    public DynamicMessage jsonToMessage(String json){
        try {
            DynamicMessage.Builder nextMessage = DynamicMessage.newBuilder(descriptor);
            jsonParser.merge(json, nextMessage);
            return nextMessage.build();
        }catch (Throwable t){
            logger.warn("Transe message error:", t);
            return null;
        }

    }

}
