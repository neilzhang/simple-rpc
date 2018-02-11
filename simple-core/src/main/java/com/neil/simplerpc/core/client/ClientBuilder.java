package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.method.listener.ClientInvocationListener;

/**
 * @author neil
 */
public class ClientBuilder {

    private String zooConn;

    private int timeout = 3000;

    private ClientInvocationListener listener;

    public ClientBuilder zoo(String conn) {
        this.zooConn = conn;
        return this;
    }

    public ClientBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public ClientBuilder addListener(ClientInvocationListener clientListener) {
        this.listener = clientListener;
        return this;
    }

    public RpcClient build() {
        assert zooConn != null;
        return new RpcClient(zooConn, timeout, listener);
    }


}
