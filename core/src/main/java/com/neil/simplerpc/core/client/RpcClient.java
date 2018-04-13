package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.client.method.point.MethodDelegateInvocationPoint;
import com.neil.simplerpc.core.exception.RpcTimeoutException;
import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.method.handler.MethodInvocationHandler;
import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.registry.discovery.ServiceDiscovery;
import com.neil.simplerpc.core.service.ServiceDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 发起 Rpc 调用的客户端
 *
 * @author neil
 */
public class RpcClient {

    private static final int STATE_LATENT = 0;

    private static final int STATE_INITIATED = 1;

    private static final int STATE_CLOSED = -1;

    private final int timeout;

    private final MethodInvocationListener listener;

    private final RequestIdGenerator idGenerator = new RequestIdGenerator();

    private final ClientContext clientContext;

    private final ServiceDiscovery serviceDiscovery;

    private volatile int state;

    public RpcClient(String zkConn, int timeout) {
        this(zkConn, timeout, null);
    }

    public RpcClient(String zkConn, int timeout, MethodInvocationListener listener) {
        this.timeout = timeout;
        this.listener = listener;
        this.clientContext = new ClientContext();
        this.serviceDiscovery = new ServiceDiscovery(zkConn);
        this.state = STATE_LATENT;
    }

    public static ClientBuilder builder() {
        return new ClientBuilder();
    }

    /**
     * 获取可以发起远程调用的服务代理
     *
     * @param service 服务类
     * @return 可以发起远程调用的服务代理
     */
    public <T> T proxy(Class<T> service) throws SimpleRpcException {
        MethodDelegateInvocationPoint point = new MethodDelegateInvocationPoint(this, service);
        Object proxy = Proxy.newProxyInstance(
                this.getClass().getClassLoader(),
                new Class[]{service},
                new MethodInvocationHandler(point, this.listener));
        ServiceDescriptor descriptor = new ServiceDescriptor(service.getName());
        this.serviceDiscovery.subscribe(descriptor, this.clientContext);
        return (T) proxy;
    }

    /**
     * 发起远程请求调用
     *
     * @param service    服务类
     * @param method     调用方法
     * @param parameters 传入参数
     * @return
     * @throws RpcTimeoutException 当远程调用超时，将抛出改异常
     * @throws SimpleRpcException  当发生调用异常时，将抛出改异常
     */
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
        return clientContext.createResponseFuture(request, timeout);
    }

    /**
     * 初始化客户端
     */
    public void init() {
        if (this.state == STATE_LATENT) {
            this.state = STATE_INITIATED;
            this.serviceDiscovery.start();
        }
    }

    /**
     * 关闭客户端
     */
    public void close() {
        if (this.state == STATE_INITIATED) {
            this.state = STATE_CLOSED;
            this.serviceDiscovery.close();
            this.clientContext.destroy();
        }
    }

}
