package com.neil.simplerpc.core.method.listener;


import com.neil.simplerpc.core.method.point.MethodInvocationPoint;

/**
 * @author neil
 */
public interface MethodInvocationListener {

    Object around(MethodInvocationPoint point) throws Throwable;

}
