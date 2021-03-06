package com.neil.simplerpc.core.server.method.point;

import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodProxyInvocationPoint implements MethodInvocationPoint {

    private Object target;

    private Method method;

    private Object[] args;

    public Object proceed() throws Throwable {
        return method.invoke(target, args);
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
