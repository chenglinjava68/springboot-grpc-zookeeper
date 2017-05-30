package io.grpc.examples;

import com.alibaba.fastjson.JSON;
import com.gin.zookeeper.loadbalance.RandomLoadBalanceStrategy;
import io.grpc.ManagedChannel;
import io.grpc.examples.helloworld.HelloWorldServiceGrpc;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import java.util.logging.Logger;

/**
 * Created by shenhui.ysh on 2017/5/27 0027.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        long st = System.currentTimeMillis();
        /** 连接池的配置 */
        GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();

        /** 下面的配置均为默认配置,默认配置的参数可以在BaseObjectPoolConfig中找到 */
        poolConfig.setMaxTotal(8); // 池中的最大连接数
        poolConfig.setMinIdle(0); // 最少的空闲连接数
        poolConfig.setMaxIdle(8); // 最多的空闲连接数
        poolConfig.setMaxWaitMillis(-1); // 当连接池资源耗尽时,调用者最大阻塞的时间,超时时抛出异常 单位:毫秒数
        poolConfig.setLifo(true); // 连接池存放池化对象方式,true放在空闲队列最前面,false放在空闲队列最后
        poolConfig.setMinEvictableIdleTimeMillis(1000L * 60L * 30L); // 连接空闲的最小时间,达到此值后空闲连接可能会被移除,默认即为30分钟
        poolConfig.setBlockWhenExhausted(true); // 连接耗尽时是否阻塞,默认为true

        /** 连接池创建 */
        GenericObjectPool<HelloWorldClient> genericObjectPool = new GenericObjectPool<HelloWorldClient>(new GrpcClientPoolFactory("grpc", HelloWorldServiceGrpc.HelloWorldServiceImplBase.class,
                "127.0.0.1", "daily", "1.0.0", new RandomLoadBalanceStrategy()), poolConfig);
        logger.info("init genericObjectPool cost : "+(System.currentTimeMillis() - st));

        st = System.currentTimeMillis();
        HelloWorldClient client = genericObjectPool.borrowObject();
        logger.info("init client cost : "+(System.currentTimeMillis() - st));

        for (int i = 0 ; i<=5 ; ++i) {
            st = System.currentTimeMillis();
            HelloWorldClient clientTest = genericObjectPool.borrowObject();
            logger.info("init client cost : "+(System.currentTimeMillis() - st));
        }

        st = System.currentTimeMillis();
        int i = 1;
        while (i > 0) {
            String user = "world";
            if (args.length > 0) {
                user = args[0];
            }
            client.sayHello(user);
            logger.info(i + " cost : " + (System.currentTimeMillis() - st));
            --i;
        }
    }

}
