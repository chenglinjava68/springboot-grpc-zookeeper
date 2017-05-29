package com.springboot.grpc.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("grpc")
public class GRpcServerProperties {
    private int port = 6565;
    /**
     * 权重
     */
    private int weight;
    /**
     * 是否对外提供服务
     */
    private boolean status = true;
    /**
     * 表示服务是否只是直接调用.true表示直接调用,false表示会额外注册到zk上
     */
    private boolean directInvoke = false;
    /**
     * 服务版本号
     */
    private String version = "1.0.0";
    /**
     * 服务集群组名,默认为gRPC,如果服务部署两套环境,推荐设置该参数
     */
    private String group = "daily";
    /**
     * zk ip:port
     */
    private String zkAddress;
    /**
     * zk 超时时间
     */
    private int zkTimeout = 3000;
    /**
     * 重试之间初始等待时间
     */
    private int baseSleepTimeMs = 10;
    /**
     * 重试之间最长等待时间
     */
    private int maxSleepTimeMs = 1000;
    /**
     * 重试次数
     */
    private int maxRetries = 3;
    /**
     * gRPC服务名,默认为grpc,服务发现端必须是同样的服务名
     */
    private String serviceName = "grpc";

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public boolean isDirectInvoke() {
        return directInvoke;
    }

    public void setDirectInvoke(boolean directInvoke) {
        this.directInvoke = directInvoke;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getZkAddress() {
        return zkAddress;
    }

    public void setZkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public int getZkTimeout() {
        return zkTimeout;
    }

    public void setZkTimeout(int zkTimeout) {
        this.zkTimeout = zkTimeout;
    }

    public int getBaseSleepTimeMs() {
        return baseSleepTimeMs;
    }

    public void setBaseSleepTimeMs(int baseSleepTimeMs) {
        this.baseSleepTimeMs = baseSleepTimeMs;
    }

    public int getMaxSleepTimeMs() {
        return maxSleepTimeMs;
    }

    public void setMaxSleepTimeMs(int maxSleepTimeMs) {
        this.maxSleepTimeMs = maxSleepTimeMs;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public String toString() {
        return "GRpcServerProperties{" +
                "port=" + port +
                ", weight=" + weight +
                ", status=" + status +
                ", directInvoke=" + directInvoke +
                ", version='" + version + '\'' +
                ", group='" + group + '\'' +
                ", zkAddress='" + zkAddress + '\'' +
                ", zkTimeout=" + zkTimeout +
                ", baseSleepTimeMs=" + baseSleepTimeMs +
                ", maxSleepTimeMs=" + maxSleepTimeMs +
                ", maxRetries=" + maxRetries +
                ", serviceName='" + serviceName + '\'' +
                '}';
    }

}
