package io.grpc.examples;

import com.alibaba.fastjson.JSON;
import com.gin.zookeeper.ZkConfig;
import com.gin.zookeeper.ZkRegistry;
import com.gin.zookeeper.constants.RegistryConstants;
import com.gin.zookeeper.listener.ZkNotifyListener;
import com.gin.zookeeper.loadbalance.ILoadBalanceStrategy;
import com.gin.zookeeper.pojo.InvokeConn;
import com.gin.zookeeper.pojo.ProviderInfo;
import com.gin.zookeeper.pojo.ServiceInfo;
import com.gin.zookeeper.utils.LocalIpUtils;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by shenhui.ysh on 2017/5/30 0030.
 */
public abstract class GrpcBaseClientPoolFactory<T> implements PooledObjectFactory<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcBaseClientPoolFactory.class);

    /**
     * 本机信息
     */
    protected static final ProviderInfo PROVIDER_INFO = new ProviderInfo(LocalIpUtils.getLocalIp(), 0);
    /**
     * 对PROVIDER_CONN_CONCURRENT_MAP操作时,需要获取锁.读不需要
     */
    protected final Object PROVIDER_CONN_LOCK = new Object();
    /**
     * service对外提供服务的provider的连接
     */
    protected final ConcurrentHashMap<ProviderInfo, InvokeConn> PROVIDER_CONN_CONCURRENT_MAP = new ConcurrentHashMap<ProviderInfo, InvokeConn>();
    /**
     * 所有服务的list表,冗余PROVIDER_CONN_CONCURRENT_MAP,便于获取连接时,直接获取
     */
    protected final CopyOnWriteArrayList<InvokeConn> PROVIDER_CONN_LIST = new CopyOnWriteArrayList<InvokeConn>();

    public GrpcBaseClientPoolFactory(){}

    public GrpcBaseClientPoolFactory(String serviceName, Class interfaceClazz, String zkAddress, String group, String version){
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
            LOGGER.error("GrpcBaseClientPoolFactory initZkConsumer error:{}", t.getMessage() ,t);
        }
    }

    @Override
    public void activateObject(PooledObject<T> p) throws Exception {
    }

    @Override
    public void passivateObject(PooledObject<T> p) throws Exception {
    }

    /**
     * 初始化consumer的zk
     * @param zkConfig
     * @param serviceInfo
     * @param serviceName
     * @throws Exception
     */
    protected void initZkConsumer(ZkConfig zkConfig, ServiceInfo serviceInfo, String serviceName) throws Exception {
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
