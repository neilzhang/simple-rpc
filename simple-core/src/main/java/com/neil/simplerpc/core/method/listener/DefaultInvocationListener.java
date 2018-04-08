package com.neil.simplerpc.core.method.listener;

import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

/**
 * @author neil
 */
public class DefaultInvocationListener implements MethodInvocationListener {

    @Override
    public Object around(MethodInvocationPoint point) throws Throwable {
        return point.proceed();
    }

}
