package io.grpc.examples;

import com.alibaba.fastjson.JSON;
import com.gin.zookeeper.constants.RegistryConstants;
import com.gin.zookeeper.pojo.ProviderInfo;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.examples.helloworld.HelloReply;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloWorldServiceGrpc;

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
    private GrpcServicerAddressService grpcServicerAddressService;

    public HelloWorldClient(String zkAddress, String group, String version) {
        grpcServicerAddressService = new GrpcServicerAddressService("grpc");
        ProviderInfo providerInfo = grpcServicerAddressService.fetchGrpcServicerAddress(zkAddress, HelloWorldServiceGrpc.HelloWorldServiceImplBase.class, group, version);
        logger.info("providerInfo:"+JSON.toJSONString(providerInfo));
        ManagedChannelBuilder<?> channelBuilder =ManagedChannelBuilder.forAddress(providerInfo.getIp(), providerInfo.getPort()).usePlaintext(true);
        channel = channelBuilder.build();
        blockingStub = HelloWorldServiceGrpc.newBlockingStub(channel);
    }

    public HelloWorldClient(String host, int port) {
        this(ManagedChannelBuilder.forAddress(host, port).usePlaintext(true));
    }

    private HelloWorldClient(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
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
