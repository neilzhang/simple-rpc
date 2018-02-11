package com.neil.simplerpc.core.method.listener;


import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

/**
 * @author neil
 */
public interface MethodInvocationListener {

    void before(MethodInvocationPoint point) throws Throwable;

    Object around(MethodInvocationPoint point) throws Throwable;

    void after(MethodInvocationPoint point) throws Throwable;

}
