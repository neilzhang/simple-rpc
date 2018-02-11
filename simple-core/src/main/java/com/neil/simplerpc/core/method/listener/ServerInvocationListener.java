package com.neil.simplerpc.core.method.listener;

import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

/**
 * @author neil
 */
public class ServerInvocationListener implements MethodInvocationListener {

    @Override
    public void before(MethodInvocationPoint point) throws Throwable {

    }

    @Override
    public Object around(MethodInvocationPoint point) throws Throwable {
        return point.proceed();
    }

    @Override
    public void after(MethodInvocationPoint point) throws Throwable {

    }

}
