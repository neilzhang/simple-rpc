package com.neil.simplerpc.core.method.point;


import com.neil.simplerpc.core.method.invocation.MethodInvocation;

import java.lang.reflect.Method;

/**
 * @author neil
 */
public interface MethodInvocationPoint extends MethodInvocation, MethodPoint {

    void setTarget(Object target);

    void setMethod(Method method);

    void setArgs(Object[] args);

}
