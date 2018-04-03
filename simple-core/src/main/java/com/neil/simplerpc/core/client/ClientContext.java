package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.service.ServiceDescriptor;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 用于临时存储 Response 的数据结构
 * <p>该类实现是线程安全的</p>
 *
 * @author neil
 */
public class ClientContext {

    /**
     * Response Map，Key 为请求 ID，Value 为 Response
     */
    private final ConcurrentHashMap<Long, Response> responseMap;

    private final ServiceContainer container;

    public ClientContext() {
        this.responseMap = new ConcurrentHashMap<>();
        this.container = new ServiceContainer(this);
    }

    /**
     * 放置 Response
     *
     * @param response 响应结果，不能为 {@code null}
     */
    public void placeResponse(Response response) {
        this.responseMap.put(response.getRequestId(), response);
    }

    /**
     * 收取 Response
     *
     * @param requestId 请求 ID
     * @return 响应结果，可能为 {@code null}
     */
    public Response fetchResponse(long requestId) {
        return this.responseMap.remove(requestId);
    }

    public ServiceContainer getServiceContainer() {
        return this.container;
    }

    public ServiceProxy getServiceProxy(ServiceDescriptor descriptor) {
        return container.getServiceProxy(descriptor);
    }

}
