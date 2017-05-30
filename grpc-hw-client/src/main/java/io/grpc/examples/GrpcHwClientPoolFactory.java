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
public class GrpcHwClientPoolFactory extends GrpcBaseClientPoolFactory<HelloWorldClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcHwClientPoolFactory.class);

    protected Class interfaceClazz;
    protected ILoadBalanceStrategy loadBalanceStrategy;

    public GrpcHwClientPoolFactory(String serviceName, Class interfaceClazz, String zkAddress, String group, String version,
                                  ILoadBalanceStrategy loadBalanceStrategy){
        super(serviceName, interfaceClazz, zkAddress, group, version);
        this.interfaceClazz = interfaceClazz;
        this.loadBalanceStrategy = loadBalanceStrategy;
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
        if (p != null && p.getObject() != null) {
            return true;
        }
        return false;
    }

}
