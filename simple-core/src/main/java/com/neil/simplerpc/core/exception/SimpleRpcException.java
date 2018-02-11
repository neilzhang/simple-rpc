package com.neil.simplerpc.core.exception;

/**
 * @author neil
 */
public class SimpleRpcException extends RuntimeException {

    public SimpleRpcException(String message) {
        super(message);
    }

    public SimpleRpcException(String message, Throwable cause) {
        super(message, cause);
    }

}
