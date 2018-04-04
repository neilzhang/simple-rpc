package com.neil.simplerpc.core.method.handler;

import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;
import com.neil.simplerpc.core.method.point.MethodProxyInvocationPoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodProxyInvocationHandler implements InvocationHandler {

    private final Object target;

    private final MethodInvocationListener listener;

    public MethodProxyInvocationHandler(Object target) {
        this(target, null);
    }

    public MethodProxyInvocationHandler(Object target, MethodInvocationListener listener) {
        this.target = target;
        this.listener = listener;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        MethodInvocationPoint point = new MethodProxyInvocationPoint();
        point.setTarget(target);
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
