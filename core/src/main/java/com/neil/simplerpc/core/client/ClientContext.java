package com.neil.simplerpc.core.client;

/**
 * 客户端上下文
 *
 * @author neil
 */
public class ClientContext {

    private final ResponseContainer responseContainer;

    /**
     * 服务容器
     */
    private final ServiceContainer serviceContainer;

    public ClientContext() {
        this.responseContainer = new ResponseContainer(this);
        this.serviceContainer = new ServiceContainer(this);
    }

    public ResponseContainer getResponseContainer() {
        return this.responseContainer;
    }

    /**
     * 获取服务容器
     *
     * @return 服务容器
     */
    public ServiceContainer getServiceContainer() {
        return this.serviceContainer;
    }

}
