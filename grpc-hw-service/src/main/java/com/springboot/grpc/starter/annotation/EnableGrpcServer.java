package com.springboot.grpc.starter.annotation;

import com.springboot.grpc.starter.GRpcAutoConfiguration;
import com.springboot.grpc.starter.config.GRpcServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Created by shenhui.ysh on 2017/5/29 0029.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(GRpcAutoConfiguration.class)
@EnableConfigurationProperties(GRpcServerProperties.class)
public @interface EnableGrpcServer {
}
