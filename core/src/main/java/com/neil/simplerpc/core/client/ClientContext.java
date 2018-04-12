package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.service.ServiceInstance;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端上下文
 *
 * @author neil
 */
public class ClientContext extends ServiceContext {

    private ServiceProxyFactory serviceProxyFactory;

    private ResponseFutureFactory responseFutureFactory;

    private ConcurrentHashMap<Long, ResponseFuture> futureMap;

    /**
     * Response Map，Key 为请求 ID，Value 为 Response
     */
    private ConcurrentHashMap<Long, Response> responseMap;

    public ClientContext() {
        this.serviceProxyFactory = new ServiceProxyFactory(this);
        this.responseFutureFactory = new ResponseFutureFactory(this);
        this.futureMap = new ConcurrentHashMap<>();
        this.responseMap = new ConcurrentHashMap<>();
    }

    @Override
    public ServiceProxy createServiceProxy(ServiceInstance instance) {
        return serviceProxyFactory.create(instance);
    }

    public ResponseFuture createResponseFuture(Request request, int timeout) {
        ResponseFuture responseFuture = responseFutureFactory.create(request, timeout);
        this.futureMap.put(request.getRequestId(), responseFuture);
        return responseFuture;
    }

    public Response getResponse(long requestId) {
        return this.responseMap.remove(requestId);
    }

    public void receiveResponse(Response response) {
        this.responseMap.put(response.getRequestId(), response);
        ResponseFuture responseFuture = this.futureMap.get(response.getRequestId());
        if (responseFuture != null) {
            responseFuture.received();
        }
    }

}
