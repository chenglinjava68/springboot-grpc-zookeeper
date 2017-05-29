package io.grpc.examples;

import com.alibaba.fastjson.JSON;
import io.grpc.examples.helloworld.HelloWorldServiceGrpc;

import java.util.logging.Logger;

/**
 * Created by shenhui.ysh on 2017/5/27 0027.
 */
public class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        long stt = System.currentTimeMillis();
        long st = System.currentTimeMillis();
        HelloWorldClient client = new HelloWorldClient("127.0.0.1", "daily", "1.0.0");
        int i = 1000;
        try {
            while (i > 0) {
                String user = "world";
                if (args.length > 0) {
                    user = args[0];
                }
                client.sayHello(user);
                logger.info(i + " cost:" + (System.currentTimeMillis() - st));
                st = System.currentTimeMillis();
                --i;
            }
        } finally {
            client.shutdown();
        }
        logger.info(i + " cost total:" + (System.currentTimeMillis() - stt));
    }

}
