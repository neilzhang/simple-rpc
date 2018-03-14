package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.method.listener.ClientInvocationListener;

/**
 * @author neil
 */
public class ClientBuilder {

    private String zkConn;

    private int timeout = 3000;

    private ClientInvocationListener listener;

    public ClientBuilder zoo(String conn) {
        this.zkConn = conn;
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
        assert zkConn != null;
        return new RpcClient(zkConn, timeout, listener);
    }


}
