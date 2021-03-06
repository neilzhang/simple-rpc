package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.method.listener.MethodInvocationListener;

/**
 * Client 构建器
 *
 * @author neil
 */
public class ClientBuilder {

    /**
     * zookeeper 服务中心地址
     */
    private String zkConn;

    /**
     * 请求超时时间
     */
    private int timeout = 3000;

    /**
     * 客户端调用监听器
     */
    private MethodInvocationListener listener;

    /**
     * 配置 zookeeper 服务中心地址
     *
     * @param zkConn zookeeper 服务中心地址
     * @return Client 构建器
     */
    public ClientBuilder zkCoon(String zkConn) {
        this.zkConn = zkConn;
        return this;
    }

    /**
     * 配置请求超时时间
     *
     * @param timeout 请求超时时间
     * @return Client 构建器
     */
    public ClientBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * 配置客户端调用监听器
     *
     * @param clientListener 客户端调用监听器
     * @return Client 构建器
     */
    public ClientBuilder addListener(MethodInvocationListener clientListener) {
        this.listener = clientListener;
        return this;
    }

    /**
     * 构建 RpcClient
     *
     * @return RpcClient 实例
     */
    public RpcClient build() {
        return new RpcClient(this.zkConn, this.timeout, this.listener);
    }


}
