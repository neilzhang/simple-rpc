package com.neil.simplerpc.core.exception;

/**
 * @author neil
 */
public class RpcTimeoutException extends RuntimeException {

    public RpcTimeoutException(String message) {
        super(message);
    }

    public RpcTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
