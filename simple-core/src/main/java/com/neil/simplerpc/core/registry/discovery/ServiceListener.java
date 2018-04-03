package com.neil.simplerpc.core.registry.discovery;

import com.neil.simplerpc.core.service.ServiceInstance;

/**
 * @author neil
 */
public interface ServiceListener {

    void onAdd(ServiceInstance serviceInstance);

    void onRemove(ServiceInstance serviceInstance);

}
