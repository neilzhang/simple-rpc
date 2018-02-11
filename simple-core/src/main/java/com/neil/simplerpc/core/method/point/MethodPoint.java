package com.neil.simplerpc.core.method.point;

import java.lang.reflect.Method;

/**
 * @author neil
 */
public interface MethodPoint {

    Object getTarget();

    Method getMethod();

    Object[] getArgs();

}
