package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.exception.RpcTimeoutException;

/**
 * @author neil
 */
public class ResponseShelf {

    private final long timeout;

    public ResponseShelf(long timeout) {
        this.timeout = timeout;
    }

    public void place(Response response) {

    }

    public Response fetch(Request request) throws RpcTimeoutException {
        return new Response();
    }
}
