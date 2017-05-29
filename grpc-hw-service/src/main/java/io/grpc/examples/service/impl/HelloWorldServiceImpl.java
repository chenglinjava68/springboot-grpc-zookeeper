package io.grpc.examples.service.impl;

import com.springboot.grpc.starter.annotation.GRpcService;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloWorldServiceGrpc;
import io.grpc.examples.interceptor.LogInterceptor;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by shenhui.ysh on 2017/5/26 0026.
 */
@GRpcService(interceptors = {LogInterceptor.class})
public class HelloWorldServiceImpl extends HelloWorldServiceGrpc.HelloWorldServiceImplBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(HelloWorldServiceImpl.class);

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloReply> responseObserver) {
        LOGGER.info("HelloWorldServiceImpl sayHello:{}", request.getName());
        final HelloReply.Builder replyBuilder = HelloReply.newBuilder().setMessage("Hello " + request.getName());
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

}
