package com.neil.simplerpc.core.server;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全
 *
 * @author neil
 */
public class ServerContext {

    private final ConcurrentHashMap<String, Object> beanMap;

    private final ConcurrentHashMap<String, Method> methodMap;

    private final Object methodMapLock = new Object();

    public ServerContext() {
        this.beanMap = new ConcurrentHashMap<>();
        this.methodMap = new ConcurrentHashMap<>();
    }

    public Object getBean(String serviceName) {
        return beanMap.get(serviceName);
    }

    public void registerBean(Class<?> service, Object bean) {
        beanMap.put(service.getName(), bean);
    }

    public Method getMethod(Object bean, String methodName, Class<?>[] parameterTypes) throws NoSuchMethodException {
        String signature = getSignature(bean, methodName, parameterTypes);
        Method method = methodMap.get(signature);
        if (method == null) {
            synchronized (methodMapLock) {
                method = methodMap.get(signature);
                if (method == null) {
                    method = bean.getClass().getDeclaredMethod(methodName, parameterTypes);
                    methodMap.put(signature, method);
                }
            }
        }
        return method;
    }

    private String getSignature(Object bean, String methodName, Class<?>[] parameterTypes) {
        StringBuilder signature = new StringBuilder();
        String serviceName = bean.getClass().getName();
        signature.append(serviceName);
        signature.append('.');
        signature.append(methodName);
        signature.append('(');
        if (parameterTypes != null) {
            for (Class<?> parameterType : parameterTypes) {
                signature.append(parameterType.getName());
                if (!parameterType.equals(parameterTypes[parameterTypes.length - 1])) {
                    signature.append(',');
                }
            }
        }
        signature.append(')');
        return signature.toString();
    }

}
