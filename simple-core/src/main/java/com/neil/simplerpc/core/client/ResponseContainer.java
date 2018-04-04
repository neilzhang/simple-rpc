package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author neil
 */
public class ResponseContainer {

    private final ClientContext clientContext;

    private ConcurrentHashMap<Long, ResponseFuture> futureMap;

    /**
     * Response Map，Key 为请求 ID，Value 为 Response
     */
    private ConcurrentHashMap<Long, Response> responseMap;

    public ResponseContainer(ClientContext clientContext) {
        this.clientContext = clientContext;
        this.futureMap = new ConcurrentHashMap<>();
        this.responseMap = new ConcurrentHashMap<>();
    }

    public Response get(long requestId) {
        return this.responseMap.remove(requestId);
    }

    public void receive(Response response) {
        this.responseMap.put(response.getRequestId(), response);
        ResponseFuture responseFuture = this.futureMap.get(response.getRequestId());
        if (responseFuture != null) {
            responseFuture.received();
        }
    }

    public ResponseFuture create(Request request, int timeout) {
        ResponseFuture responseFuture = new ResponseFuture(request, timeout, this.clientContext);
        this.futureMap.put(request.getRequestId(), responseFuture);
        return responseFuture;
    }

}
