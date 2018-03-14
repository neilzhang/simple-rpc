package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.exception.RpcTimeoutException;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.method.handler.MethodDelegateInvocationHandler;
import com.neil.simplerpc.core.method.listener.ClientInvocationListener;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author neil
 */
public class RpcClient {

    private int timeout = 3000;

    private ClientInvocationListener listener;

    private RequestIdGenerator idGenerator = new RequestIdGenerator();

    private ConcurrentHashMap<Class<?>, ServiceChannel> serviceChannelMap = new ConcurrentHashMap<>();

    private CuratorFramework zkClient;

    public RpcClient(String zkConn, int timeout) {
        this(zkConn, timeout, null);
    }

    public RpcClient(String zkConn, int timeout, ClientInvocationListener listener) {
        this.timeout = timeout;
        this.listener = listener;

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 3);
        zkClient = CuratorFrameworkFactory.newClient(zkConn, retryPolicy);
        zkClient.start();
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    public <T> T proxy(Class<T> service) {
        Object proxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{service},
                new MethodDelegateInvocationHandler(this, service, listener));
        ServiceChannel serviceChannel = new ServiceChannel(service, zkClient);
        serviceChannelMap.put(service, serviceChannel);
        return (T) proxy;
    }

    public ResponseFuture call(Class<?> service, Method method, Object[] parameters) throws RpcTimeoutException, SimpleRpcException {
        Request request = new Request();
        request.setServiceName(service.getName());
        request.setMethodName(method.getName());
        request.setParameterTypes(method.getParameterTypes());
        request.setParameters(parameters);
        request.setRequestId(idGenerator.get());
        ServiceChannel serviceChannel = serviceChannelMap.get(service);
        if (serviceChannel == null) {
            throw new SimpleRpcException("there are no available service channel. Service: `" + service + "`. Method: `" + method + "`. Parameters: `" + parameters + "`.");
        }
        serviceChannel.call(request);
        return new ResponseFuture(request, timeout);
    }

    public void close() {
        for (ServiceChannel serviceChannel : serviceChannelMap.values()) {
            serviceChannel.close();
        }
    }

}
