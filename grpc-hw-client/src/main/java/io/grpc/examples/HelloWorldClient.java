package io.grpc.examples;

import com.alibaba.fastjson.JSON;
import com.gin.zookeeper.constants.RegistryConstants;
import com.gin.zookeeper.loadbalance.RandomLoadBalanceStrategy;
import com.gin.zookeeper.pojo.ProviderInfo;
import com.google.common.base.Joiner;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloWorldServiceGrpc;
import org.apache.commons.pool2.impl.GenericObjectPool;

import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by shenhui.ysh on 2017/5/25 0025.
 */
public class HelloWorldClient {
    private static final Logger logger = Logger.getLogger(HelloWorldClient.class.getName());

    private final ManagedChannel channel;
    private final HelloWorldServiceGrpc.HelloWorldServiceBlockingStub blockingStub;

    public HelloWorldClient(ManagedChannel channel) throws Exception {
        this.channel = channel;
        blockingStub = HelloWorldServiceGrpc.newBlockingStub(channel);
    }

    public HelloWorldClient(String host, int port) {
        channel = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true).build();
        blockingStub = HelloWorldServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public void sayHello(String name) {
        logger.info("Will try to greet " + name + " ...");
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloReply response;
        try {
            response = blockingStub.sayHello(request);
        } catch (StatusRuntimeException e) {
            logger.log(Level.WARNING, "RPC failed: {0}", e.getStatus());
            return;
        }
        logger.info("Greeting: " + response.getMessage());
    }

}
