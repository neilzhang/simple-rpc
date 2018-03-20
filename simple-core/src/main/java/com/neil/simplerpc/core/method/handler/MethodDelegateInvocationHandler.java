package com.neil.simplerpc.core.method.handler;

import com.neil.simplerpc.core.client.RpcClient;
import com.neil.simplerpc.core.method.listener.ClientInvocationListener;
import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.method.point.MethodDelegateMethodInvocationPoint;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodDelegateInvocationHandler implements InvocationHandler {

    private final MethodInvocationListener listener;

    private final RpcClient rpcClient;

    private final Class<?> service;

    public MethodDelegateInvocationHandler(RpcClient rpcClient, Class<?> service, ClientInvocationListener listener) {
        this.listener = listener;
        this.rpcClient = rpcClient;
        this.service = service;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocationPoint point = new MethodDelegateMethodInvocationPoint(rpcClient, service);
        point.setMethod(method);
        point.setArgs(args);
        if (listener == null) {
            return point.proceed();
        } else {
            listener.before(point);
            Object result = listener.around(point);
            listener.after(point);
            return result;
        }
    }

}
