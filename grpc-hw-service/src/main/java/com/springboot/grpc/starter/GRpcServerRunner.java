package com.springboot.grpc.starter;

import com.gin.zookeeper.ZkConfig;
import com.gin.zookeeper.ZkRegistry;
import com.gin.zookeeper.constants.RegistryConstants;
import com.gin.zookeeper.pojo.ProviderInfo;
import com.gin.zookeeper.pojo.ServiceInfo;
import com.gin.zookeeper.utils.LocalIpUtils;
import com.springboot.grpc.starter.annotation.GRpcGlobalInterceptor;
import com.springboot.grpc.starter.annotation.GRpcService;
import com.springboot.grpc.starter.config.GRpcServerProperties;
import io.grpc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.type.StandardMethodMetadata;

import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GRpcServerRunner implements CommandLineRunner, DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(GRpcServerRunner.class);

    @Autowired
    private AbstractApplicationContext applicationContext;

    @Autowired
    private GRpcServerProperties gRpcServerProperties;

    private GRpcServerBuilderConfigurer configurer;

    private Server server;

    private ZkRegistry registry;

    public GRpcServerRunner(GRpcServerBuilderConfigurer configurer) {
        this.configurer = configurer;
    }

    @Override
    public void run(String... args) {
        ProviderInfo providerInfo = null;
        ServiceInfo serviceInfo = null;
        try {
            // 如果设置为zk注册,则先注册服务; 如果启动gRpc server失败,则注销该服务
            providerInfo = new ProviderInfo();
            providerInfo.setIp(LocalIpUtils.getLocalIp());
            providerInfo.setPort(gRpcServerProperties.getPort());
            providerInfo.setWeight(gRpcServerProperties.getWeight());
            providerInfo.setStatus(gRpcServerProperties.isStatus());

            serviceInfo = new ServiceInfo();
            serviceInfo.setDirectInvoke(gRpcServerProperties.isDirectInvoke());
            serviceInfo.setGroup(gRpcServerProperties.getGroup());
            serviceInfo.setVersion(gRpcServerProperties.getVersion());

            LOGGER.info("Starting gRPC Server");
            Collection<ServerInterceptor> globalInterceptors = getBeanNamesByTypeWithAnnotation(GRpcGlobalInterceptor.class, ServerInterceptor.class)
                    .map(name -> applicationContext.getBeanFactory().getBean(name, ServerInterceptor.class))
                    .collect(Collectors.toList());

            final ServerBuilder<?> serverBuilder = ServerBuilder.forPort(gRpcServerProperties.getPort());
            final List<BindableService> bindableServiceList = new ArrayList<>();

            // find and register all GRpcService-enabled beans
            getBeanNamesByTypeWithAnnotation(GRpcService.class, BindableService.class)
                    .forEach(name -> {
                        BindableService srv = applicationContext.getBeanFactory().getBean(name, BindableService.class);
                        ServerServiceDefinition serviceDefinition = srv.bindService();
                        GRpcService gRpcServiceAnn = applicationContext.findAnnotationOnBean(name, GRpcService.class);
                        serviceDefinition = bindInterceptors(serviceDefinition, gRpcServiceAnn, globalInterceptors);
                        serverBuilder.addService(serviceDefinition);
                        LOGGER.info("'{}' service has been registered", srv.getClass().getName());
                        bindableServiceList.add(srv);
                    });

            if (!serviceInfo.isDirectInvoke()) {
                for (BindableService srv : bindableServiceList) {
                    serviceInfo.setInterfaceClazz(srv.getClass().getSuperclass());
                    zkRegister(providerInfo, serviceInfo);
                    LOGGER.info("'{}' service has been registered to zookeeper", srv.getClass().getName());
                }
            }

            configurer.configure(serverBuilder);
            server = serverBuilder.build().start();
            LOGGER.info("gRPC Server started, listening on port {}", gRpcServerProperties.getPort());
            startDaemonAwaitThread();
        }catch (Throwable t) {
            LOGGER.error("GRpcServerRunner run error:{}", t.getMessage(), t);
            unZkRegister(providerInfo, serviceInfo);
        }
    }

    /**
     * 启动失败需要将注册的服务注销掉
     */
    private void unZkRegister(ProviderInfo providerInfo, ServiceInfo serviceInfo) {
        if (registry == null || providerInfo == null || serviceInfo == null) {
            LOGGER.error("registry is null, can not unregister zk service");
            return;
        }
        registry.unregister(serviceInfo, providerInfo, gRpcServerProperties.getServiceName(), RegistryConstants.DEFAULT_INVOKER_PROVIDER);
    }

    /**
     * 注册gRPC服务到指定zk集群上
     */
    private void zkRegister(ProviderInfo providerInfo, ServiceInfo serviceInfo) throws Exception {
        ZkConfig zkConfig = new ZkConfig();
        zkConfig.setBaseSleepTimeMs(gRpcServerProperties.getBaseSleepTimeMs());
        zkConfig.setMaxRetries(gRpcServerProperties.getMaxRetries());
        zkConfig.setMaxSleepTimeMs(gRpcServerProperties.getMaxSleepTimeMs());
        zkConfig.setZkAddress(gRpcServerProperties.getZkAddress());
        zkConfig.setZkTimeout(gRpcServerProperties.getZkTimeout());

        registry = new ZkRegistry(zkConfig);
        registry.register(serviceInfo, providerInfo, gRpcServerProperties.getServiceName(), RegistryConstants.DEFAULT_INVOKER_PROVIDER);
    }

    private ServerServiceDefinition bindInterceptors(ServerServiceDefinition serviceDefinition, GRpcService gRpcService, Collection<ServerInterceptor> globalInterceptors) {
        Stream<? extends ServerInterceptor> privateInterceptors = Stream.of(gRpcService.interceptors())
                .map(interceptorClass -> {
                    try {
                        return 0 < applicationContext.getBeanNamesForType(interceptorClass).length ?
                                applicationContext.getBean(interceptorClass) :
                                interceptorClass.newInstance();
                    } catch (Exception e) {
                        throw new BeanCreationException("Failed to create interceptor instance.", e);
                    }
                });

        List<ServerInterceptor> interceptors = Stream.concat(
                gRpcService.applyGlobalInterceptors() ? globalInterceptors.stream() : Stream.empty(),
                privateInterceptors)
                .distinct()
                .collect(Collectors.toList());
        return ServerInterceptors.intercept(serviceDefinition, interceptors);
    }

    private void startDaemonAwaitThread() {
        Thread awaitThread = new Thread() {
            @Override
            public void run() {
                try {
                    GRpcServerRunner.this.server.awaitTermination();
                } catch (InterruptedException e) {
                    LOGGER.error("gRPC server stopped", e);
                }
            }

        };
        awaitThread.setDaemon(false);
        awaitThread.start();
    }

    @Override
    public void destroy() throws Exception {
        LOGGER.info("Shutting down gRPC server");
        Optional.ofNullable(server).ifPresent(Server::shutdown);
        LOGGER.info("gRPC server stopped");
    }

    private <T> Stream<String> getBeanNamesByTypeWithAnnotation(Class<? extends Annotation> annotationType, Class<T> beanType) throws Exception {

        return Stream.of(applicationContext.getBeanNamesForType(beanType))
                .filter(name -> {
                    final BeanDefinition beanDefinition = applicationContext.getBeanFactory().getBeanDefinition(name);
                    final Map<String, Object> beansWithAnnotation = applicationContext.getBeansWithAnnotation(annotationType);

                    if (!beansWithAnnotation.isEmpty()) {
                        return beansWithAnnotation.containsKey(name);
                    } else if (beanDefinition.getSource() instanceof StandardMethodMetadata) {
                        StandardMethodMetadata metadata = (StandardMethodMetadata) beanDefinition.getSource();
                        return metadata.isAnnotated(annotationType.getName());
                    }

                    return false;
                });
    }

}
