package com.neil.simplerpc.core.method.point;

import com.neil.simplerpc.core.client.RpcClient;

import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodDelegateMethodInvocationPoint implements MethodInvocationPoint {

    private RpcClient rpcClient;

    private Class<?> service;

    private Object target;

    private Method method;

    private Object[] args;

    private DelegatedMethod delegatedMethod;

    public MethodDelegateMethodInvocationPoint(RpcClient rpcClient, Class<?> service) {
        this.rpcClient = rpcClient;
        this.service = service;
        this.delegatedMethod = new DelegatedMethodImpl();
    }

    @Override
    public Object proceed() throws Throwable {
        return delegatedMethod.call(service, method, args);
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

    interface DelegatedMethod {

        Object call(Class<?> service, Method method, Object[] args);

    }

    class DelegatedMethodImpl implements DelegatedMethod {

        @Override
        public Object call(Class<?> service, Method method, Object[] args) {
            return rpcClient.call(service, method, args);
        }

    }

}
