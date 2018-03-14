package com.neil.simplerpc.core.server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 线程安全
 *
 * @author neil
 */
public class ServicePool {

    private ConcurrentHashMap<String, Object> serviceMap = new ConcurrentHashMap<>();

    public Object getService(String serviceName) {
        return serviceMap.get(serviceName);
    }

    public void add(String serviceName, Object service) {
        serviceMap.put(serviceName, service);
    }

}
