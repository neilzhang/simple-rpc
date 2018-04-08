package com.neil.simplerpc.core.method.handler;

import com.neil.simplerpc.core.method.listener.MethodInvocationListener;
import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * @author neil
 */
public class MethodInvocationHandler implements InvocationHandler {

    private MethodInvocationPoint point;

    private MethodInvocationListener listener;

    public MethodInvocationHandler(MethodInvocationPoint point, MethodInvocationListener listener) {
        this.point = point;
        this.listener = listener;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        point.setMethod(method);
        point.setArgs(args);
        if (listener == null) {
            return point.proceed();
        } else {
            return listener.around(point);
        }
    }

}
