package com.springboot.grpc.starter;

import com.springboot.grpc.starter.annotation.GRpcService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

public class GRpcAutoConfiguration {

    @Bean
    @ConditionalOnBean(annotation = GRpcService.class)
    public GRpcServerRunner grpcServerRunner(GRpcServerBuilderConfigurer configurer) {
        return new GRpcServerRunner(configurer);
    }

    @Bean
    @ConditionalOnMissingBean(GRpcServerBuilderConfigurer.class)
    public GRpcServerBuilderConfigurer serverBuilderConfigurer() {
        return new GRpcServerBuilderConfigurer();
    }

}
