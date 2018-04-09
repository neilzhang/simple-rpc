package com.neil.simplerpc.core.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全
 *
 * @author neil
 */
public class ServerContext {

    private final ConcurrentHashMap<String, Object> beanMap = new ConcurrentHashMap<>();

    public Object getBean(String serviceName) {
        return beanMap.get(serviceName);
    }

    public void registerBean(Class<?> service, Object bean) {
        beanMap.put(service.getName(), bean);
    }

}
