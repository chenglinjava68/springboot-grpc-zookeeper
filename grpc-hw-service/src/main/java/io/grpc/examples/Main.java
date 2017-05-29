package io.grpc.examples;

import com.springboot.grpc.starter.annotation.EnableGrpcServer;
import io.grpc.examples.service.impl.HelloWorldServiceImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Created by shenhui.ysh on 2017/5/26 0026.
 */
@SpringBootApplication
@EnableGrpcServer
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

}
