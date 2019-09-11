package com.gexiang.client;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import io.grpc.reflection.v1alpha.ListServiceResponse;
import io.grpc.reflection.v1alpha.ServerReflectionRequest;
import io.grpc.reflection.v1alpha.ServerReflectionResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListServicesHandler implements StreamObserver<ServerReflectionResponse> {
    private static final Logger logger = LoggerFactory.getLogger(ListServicesHandler.class);
    private static final ServerReflectionRequest LIST_SERVICES_REQUEST = ServerReflectionRequest.newBuilder()
            .setListServices("").build();
    private final SettableFuture<ImmutableList<String>> resultFuture;
    private StreamObserver<ServerReflectionRequest> requestStream;

    public ListServicesHandler() {
        resultFuture = SettableFuture.create();
    }

    ListenableFuture<ImmutableList<String>> start(StreamObserver<ServerReflectionRequest> requestStream) {
        this.requestStream = requestStream;
        requestStream.onNext(LIST_SERVICES_REQUEST);
        return resultFuture;
    }

    @Override
    public void onNext(ServerReflectionResponse serverReflectionResponse) {
        ServerReflectionResponse.MessageResponseCase responseCase = serverReflectionResponse.getMessageResponseCase();
        switch (responseCase) {
            case LIST_SERVICES_RESPONSE:
                handleListServiceRespones(serverReflectionResponse.getListServicesResponse());
                break;
            default:
                logger.warn("Got unknown reflection response type: " + responseCase);
                break;
        }
    }

    @Override
    public void onError(Throwable t) {
        resultFuture.setException(new RuntimeException("Error in server reflection rpc while listing services", t));
    }

    @Override
    public void onCompleted() {
        if (!resultFuture.isDone()) {
            logger.error("Unexpected completion of server reflection rpc while listing services");
            resultFuture.setException(new RuntimeException("Unexpected end of rpc"));
        }
    }

    private void handleListServiceRespones(ListServiceResponse response) {
        ImmutableList.Builder<String> servicesBuilder = ImmutableList.builder();
        response.getServiceList().forEach(service -> servicesBuilder.add(service.getName()));
        resultFuture.set(servicesBuilder.build());
        requestStream.onCompleted();
    }
}
