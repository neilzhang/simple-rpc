package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.exception.RpcTimeoutException;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.method.handler.MethodDelegateInvocationHandler;
import com.neil.simplerpc.core.method.listener.ClientInvocationListener;
import com.neil.simplerpc.core.registry.discovery.ServiceDiscovery;
import com.neil.simplerpc.core.service.ServiceDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author neil
 */
public class RpcClient {

    private int timeout = 3000;

    private ClientInvocationListener listener;

    private RequestIdGenerator idGenerator = new RequestIdGenerator();

    private ClientContext clientContext;

    private ServiceDiscovery serviceDiscovery;

    private static final int STATE_CLOSED = -1;

    private static final int STATE_STARTED = 1;

    private static final int STATE_INITIATED = 0;

    private static volatile int state = STATE_INITIATED;

    public RpcClient(String zkConn, int timeout) {
        this(zkConn, timeout, null);
    }

    public RpcClient(String zkConn, int timeout, ClientInvocationListener listener) {
        this.timeout = timeout;
        this.listener = listener;
        this.clientContext = new ClientContext();
        if (zkConn != null) {
            this.serviceDiscovery = new ServiceDiscovery(zkConn);
        }
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    public <T> T proxy(Class<T> service) {
        Object proxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{service},
                new MethodDelegateInvocationHandler(this, service, this.listener));
        ServiceDescriptor descriptor = new ServiceDescriptor(service.getName());
        if (this.serviceDiscovery != null) {
            this.serviceDiscovery.subscribe(descriptor, this.clientContext.getServiceContainer());
        }
        return (T) proxy;
    }

    public ResponseFuture call(Class<?> service, Method method, Object[] parameters) throws RpcTimeoutException, SimpleRpcException {
        Request request = new Request();
        request.setServiceName(service.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(parameters);
        request.setRequestId(this.idGenerator.get());
        ServiceDescriptor descriptor = new ServiceDescriptor(service.getName());
        ServiceProxy proxy = this.clientContext.getServiceProxy(descriptor);
        if (proxy == null) {
            throw new SimpleRpcException("there is no available service proxy. service name: `" + service.getName() + "`.");
        }
        proxy.call(request);
        return new ResponseFuture(request, this.timeout, this.clientContext);
    }

    public void start() {
        if (this.state == STATE_INITIATED) {
            this.state = STATE_STARTED;
            if (this.serviceDiscovery != null) {
                this.serviceDiscovery.start();
            }
        }
    }

    public void close() {
        if (this.state == STATE_STARTED) {
            this.state = STATE_CLOSED;
            closeDiscovery();
            this.clientContext.getServiceContainer().close();
        }
    }

    private void closeDiscovery() {
        if (this.serviceDiscovery != null) {
            this.serviceDiscovery.close();
        }
    }

}
