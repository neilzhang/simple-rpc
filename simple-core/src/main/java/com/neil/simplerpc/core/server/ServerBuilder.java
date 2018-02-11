package com.neil.simplerpc.core.server;

import com.neil.simplerpc.core.method.listener.ServerInvocationListener;

/**
 * @author neil
 */
public class ServerBuilder {

    private int port = 8998;

    private String zooConn;

    private int bossThread = 0;

    private int workThread = 0;

    private ServerInvocationListener listener;

    public ServerBuilder port(int port) {
        this.port = port;
        return this;
    }

    public ServerBuilder zoo(String conn) {
        this.zooConn = conn;
        return this;
    }

    public ServerBuilder bossThread(int bossThread) {
        this.bossThread = bossThread;
        return this;
    }

    public ServerBuilder workThread(int workThread) {
        this.workThread = workThread;
        return this;
    }

    public ServerBuilder addListener(ServerInvocationListener listener) {
        this.listener = listener;
        return this;
    }

    public RpcServer build() {
        assert zooConn != null;
        return new RpcServer(port, zooConn, bossThread, workThread, listener);
    }

}
