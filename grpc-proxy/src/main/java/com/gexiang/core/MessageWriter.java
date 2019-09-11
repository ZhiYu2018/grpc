package com.gexiang.core;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import com.gexiang.vo.ProxyResponse;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.util.JsonFormat.TypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import io.grpc.stub.StreamObserver;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.MonoSink;

/**
 * A {@link StreamObserver} which writes the contents of the received messages to an
 * {@link}. The messages are writting in a newline-separated json format.
 */
public class MessageWriter implements StreamObserver<DynamicMessage> {
    private static final Logger logger = LoggerFactory.getLogger(MessageWriter.class);

    /** Used to separate the individual plaintext json proto messages. */
    private static final String MESSAGE_SEPARATOR = "\n\n";
    private final JsonFormat.Printer jsonPrinter;
    private final StringBuilder stringBuilder;
    private final MonoSink<String> tMonoSink;

    /**
     * Creates a new {@link MessageWriter} which writes the messages it sees to the supplied
     */
    public static MessageWriter create(TypeRegistry registry, MonoSink<String> tMonoSink) {
        return new MessageWriter(JsonFormat.printer().usingTypeRegistry(registry), tMonoSink);
    }

    /**
     * Returns the string representation of the stream of supplied messages. Each individual message
     * is represented as valid json, but not that the whole result is, itself, *not* valid json.
     */
    public static String writeJsonStream(ImmutableList<DynamicMessage> messages, MonoSink<String> tMonoSink) {
        return writeJsonStream(messages, TypeRegistry.getEmptyTypeRegistry(), tMonoSink);
    }

    /**
     * Returns the string representation of the stream of supplied messages. Each individual message
     * is represented as valid json, but not that the whole result is, itself, *not* valid json.
     */
    public static String writeJsonStream(ImmutableList<DynamicMessage> messages, TypeRegistry registry
    ,MonoSink<String> tMonoSink) {
        ByteArrayOutputStream resultStream = new ByteArrayOutputStream();
        MessageWriter writer = MessageWriter.create(registry, tMonoSink);
        writer.writeAll(messages);
        return resultStream.toString();
    }

    @VisibleForTesting
    MessageWriter(JsonFormat.Printer jsonPrinter, MonoSink<String> tMonoSink) {
        this.jsonPrinter = jsonPrinter;
        this.stringBuilder = new StringBuilder();
        this.tMonoSink = tMonoSink;
    }

    @Override
    public void onCompleted() {
        tMonoSink.success(stringBuilder.toString());
    }

    @Override
    public void onError(Throwable t) {
        int value = HttpStatus.SERVICE_UNAVAILABLE.value();
        String msg    = String.format("Error:%s", t.getMessage());
        tMonoSink.success(new ProxyResponse(value, msg).toJson());
    }

    @Override
    public void onNext(DynamicMessage message) {
        try {
            String msg = jsonPrinter.print(message) + MESSAGE_SEPARATOR;
            stringBuilder.append(msg);
        } catch (InvalidProtocolBufferException e) {
            logger.error("Skipping invalid response message", e);
        }
    }

    /** Writes all the supplied messages and closes the stream. */
    public void writeAll(ImmutableList<DynamicMessage> messages) {
        messages.forEach(this::onNext);
        onCompleted();
    }
}
