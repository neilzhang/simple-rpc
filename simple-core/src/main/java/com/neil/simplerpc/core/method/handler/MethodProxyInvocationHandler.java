package com.neil.simplerpc.core.method.handler;

import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;
import com.neil.simplerpc.core.method.point.MethodProxyMethodInvocationPoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodProxyInvocationHandler implements InvocationHandler {

    private final Object target;

    private final MethodInvocationListener listener;

    private final MethodInvocationPoint point;

    public MethodProxyInvocationHandler(Object target) {
        this(target, null);
    }

    public MethodProxyInvocationHandler(Object target, MethodInvocationListener listener) {
        this.target = target;
        this.listener = listener;
        this.point = new MethodProxyMethodInvocationPoint();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        point.setTarget(target);
        point.setMethod(method);
        point.setTarget(args);
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
