package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.service.ServiceInstance;

/**
 * @author neil
 */
public class ServiceProxyFactory {

    private ClientContext clientContext;

    public ServiceProxyFactory(ClientContext clientContext) {
        this.clientContext = clientContext;
    }

    public ServiceProxy get(ServiceInstance instance) {
        return new ServiceProxy(instance, clientContext);
    }
}
