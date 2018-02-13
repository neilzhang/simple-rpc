package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.exception.RpcTimeoutException;
import com.neil.simplerpc.core.method.handler.MethodDelegateInvocationHandler;
import com.neil.simplerpc.core.method.listener.ClientInvocationListener;
import com.neil.simplerpc.core.registry.ServiceRegistryCenter;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author neil
 */
public class RpcClient {

    private int timeout = 3000;

    private ClientInvocationListener listener;

    private ServiceRegistryCenter center;

    private RequestIdGenerator idGenerator = new RequestIdGenerator();

    private ChannelKeeper channelKeeper;

    public RpcClient(String zooConn, int timeout) {
        this(zooConn, timeout, null);
    }

    public RpcClient(String zooConn, int timeout, ClientInvocationListener listener) {
        this.timeout = timeout;
        this.listener = listener;
        this.center = new ServiceRegistryCenter(zooConn, this);
        this.channelKeeper = new ChannelKeeper(zooConn);
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    public void init() {
        channelKeeper.init();
    }

    public <T> T proxy(Class<T> service) {
        T proxy = (T) Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{service},
                new MethodDelegateInvocationHandler(this, service, listener));
        return proxy;
    }

    public ResponseFuture call(Class<?> service, Method method, Object[] parameters) throws RpcTimeoutException {
        Request request = new Request();
        request.setServiceName(service.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(parameters);
        request.setRequestId(idGenerator.get());
        channelKeeper.selectChannel().writeAndFlush(request);
        return new ResponseFuture(request, timeout);
    }

}
