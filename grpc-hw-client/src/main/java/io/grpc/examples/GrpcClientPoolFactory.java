package io.grpc.examples;

import com.alibaba.fastjson.JSON;
import com.gin.zookeeper.ZkConfig;
import com.gin.zookeeper.ZkRegistry;
import com.gin.zookeeper.constants.RegistryConstants;
import com.gin.zookeeper.listener.ZkNotifyListener;
import com.gin.zookeeper.loadbalance.ILoadBalanceStrategy;
import com.gin.zookeeper.pojo.Invocation;
import com.gin.zookeeper.pojo.InvokeConn;
import com.gin.zookeeper.pojo.ProviderInfo;
import com.gin.zookeeper.pojo.ServiceInfo;
import com.gin.zookeeper.utils.LocalIpUtils;
import io.grpc.Channel;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

/**
 * Created by shenhui.ysh on 2017/5/29 0029.
 */
public class GrpcClientPoolFactory implements PooledObjectFactory<HelloWorldClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClientPoolFactory.class);
    /**
     * 本机信息
     */
    private static final ProviderInfo PROVIDER_INFO = new ProviderInfo(LocalIpUtils.getLocalIp(), 0);
    /**
     * 对PROVIDER_CONN_CONCURRENT_MAP操作时,需要获取锁.读不需要
     */
    private final Object PROVIDER_CONN_LOCK = new Object();
    /**
     * service对外提供服务的provider的连接
     */
    private final ConcurrentHashMap<ProviderInfo, InvokeConn> PROVIDER_CONN_CONCURRENT_MAP = new ConcurrentHashMap<ProviderInfo, InvokeConn>();
    /**
     * 所有服务的list表,冗余PROVIDER_CONN_CONCURRENT_MAP,便于获取连接时,直接获取
     */
    private CopyOnWriteArrayList<InvokeConn> PROVIDER_CONN_LIST = new CopyOnWriteArrayList<InvokeConn>();
    private Class interfaceClazz;
    private ILoadBalanceStrategy loadBalanceStrategy;

    public GrpcClientPoolFactory(String serviceName, Class interfaceClazz, String zkAddress, String group, String version,
                                  ILoadBalanceStrategy loadBalanceStrategy){
        this.interfaceClazz = interfaceClazz;
        this.loadBalanceStrategy = loadBalanceStrategy;

        ZkConfig zkConfig = new ZkConfig();
        zkConfig.setZkAddress(zkAddress);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setInterfaceClazz(interfaceClazz);
        serviceInfo.setGroup(group);
        serviceInfo.setVersion(version);

        InvokeConn invokeConn = null;
        try {
            initZkConsumer(zkConfig, serviceInfo, serviceName);
        }catch (Throwable t) {
            LOGGER.error("GrpcChannelPoolFactory initZkConsumer error:{}", t.getMessage() ,t);
        }
    }

    /**
     * 这个地方创建连接的时候,如果创建失败,重试一次,还不行则抛出异常,把该节点暂时移除
     * @return
     * @throws Exception
     */
    @Override
    public PooledObject<HelloWorldClient> makeObject() throws Exception {
        ManagedChannel channel = null;
        Exception ex = null;
        int retryConn = 1;
        do {
            try {
                ProviderInfo providerInfo = loadBalanceStrategy.select(PROVIDER_CONN_LIST, new Invocation(interfaceClazz.getCanonicalName())).getProviderInfo();
                channel = ManagedChannelBuilder.forAddress(providerInfo.getIp(), providerInfo.getPort()).usePlaintext(true).build();
                HelloWorldClient client = new HelloWorldClient(channel);
                return new DefaultPooledObject<HelloWorldClient>(client);
            } catch (Exception e) {
                ex = e;
            }
        } while (retryConn-- > 0);
        LOGGER.error("make client object fail.e:", ex.getMessage(), ex);
        throw new Exception("make client object fail:" + ex.getMessage());
    }

    @Override
    public void destroyObject(PooledObject<HelloWorldClient> p) throws Exception {
        p.getObject().shutdown();
    }

    @Override
    public boolean validateObject(PooledObject<HelloWorldClient> p) {
        if (p!= null && p.getObject() != null) {
            return true;
        }
        return false;
    }

    @Override
    public void activateObject(PooledObject<HelloWorldClient> p) throws Exception {

    }

    @Override
    public void passivateObject(PooledObject<HelloWorldClient> p) throws Exception {

    }

    /**
     * 初始化consumer的zk
     * @param zkConfig
     * @param serviceInfo
     * @param serviceName
     * @throws Exception
     */
    private void initZkConsumer(ZkConfig zkConfig, ServiceInfo serviceInfo, String serviceName) throws Exception {
        ZkRegistry registry = new ZkRegistry(zkConfig);
        registry.register(serviceInfo, PROVIDER_INFO, serviceName, RegistryConstants.DEFAULT_INVOKER_CONSUMER);

        ZkNotifyListener listener = new ZkNotifyListener() {

            @Override
            public void notify(Set<ProviderInfo> providerInfos) {
                // 同步,保证多个listener串行更新
                synchronized (PROVIDER_CONN_LOCK) {
                    for (ProviderInfo info : providerInfos) {
                        if (!PROVIDER_CONN_CONCURRENT_MAP.containsKey(info)) {
                            InvokeConn invokeConn = new InvokeConn(info);
                            PROVIDER_CONN_CONCURRENT_MAP.putIfAbsent(info, invokeConn);
                            PROVIDER_CONN_LIST.add(invokeConn);
                        }
                    }

                    for (Map.Entry<ProviderInfo, InvokeConn> entry : PROVIDER_CONN_CONCURRENT_MAP.entrySet()) {
                        if (!providerInfos.contains(entry.getKey())) {
                            PROVIDER_CONN_LIST.remove(entry.getValue());
                            PROVIDER_CONN_CONCURRENT_MAP.remove(entry.getKey());
                        }
                    }

                    LOGGER.info("initZkConsumer PROVIDER_CONN_LIST:{}", JSON.toJSONString(PROVIDER_CONN_LIST));
                    LOGGER.info("initZkConsumer PROVIDER_CONN_CONCURRENT_MAP:{}", JSON.toJSONString(PROVIDER_CONN_CONCURRENT_MAP));
                }
            }

        };
        registry.subscribe(serviceInfo, listener, serviceName);
    }

}
