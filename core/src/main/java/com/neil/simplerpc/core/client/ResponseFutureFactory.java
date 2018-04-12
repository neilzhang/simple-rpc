package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;

/**
 * @author neil
 */
public class ResponseFutureFactory {

    private ClientContext clientContext;

    public ResponseFutureFactory(ClientContext clientContext) {
        this.clientContext = clientContext;
    }

    public ResponseFuture create(Request request, long timeout) {
        return new ResponseFuture(request, timeout, clientContext);
    }
}
