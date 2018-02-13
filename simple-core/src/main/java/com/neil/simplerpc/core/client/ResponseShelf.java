package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Response;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author neil
 */
public class ResponseShelf {

    private static final ConcurrentHashMap<Long, Response> responseMap = new ConcurrentHashMap<>();

    public static void place(Response response) {
        responseMap.put(response.getRequestId(), response);
    }

    public static Response fetch(long requestId) {
        return responseMap.remove(requestId);
    }

}
