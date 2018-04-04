package com.neil.simplerpc.core.method.point;

import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.client.ResponseFuture;
import com.neil.simplerpc.core.client.RpcClient;

import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodDelegateInvocationPoint implements MethodInvocationPoint {

    private RpcClient rpcClient;

    private Class<?> service;

    private Object target;

    private Method method;

    private Object[] args;

    public MethodDelegateInvocationPoint(RpcClient rpcClient, Class<?> service) {
        this.rpcClient = rpcClient;
        this.service = service;
    }

    @Override
    public Object proceed() throws Throwable {
        ResponseFuture responseFuture = rpcClient.call(service, method, args);
        Response response = responseFuture.get();
        if (!response.isSuccess()) {
            throw response.getThrowable();
        }
        return response.getData();
    }

    @Override
    public Object getTarget() {
        return target;
    }

    @Override
    public void setTarget(Object target) {
        this.target = target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public void setMethod(Method method) {
        this.method = method;
    }

    @Override
    public Object[] getArgs() {
        return args;
    }

    @Override
    public void setArgs(Object[] args) {
        this.args = args;
    }

}
