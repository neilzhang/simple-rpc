package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.client.ClientContext;
import com.neil.simplerpc.core.exception.RpcTimeoutException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author neil
 */
public class ResponseFuture {

    private final Request request;

    private final long timeout;

    private final ClientContext clientContext;

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition done = lock.newCondition();

    public ResponseFuture(Request request, long timeout, ClientContext clientContext) {
        this.request = request;
        this.timeout = timeout;
        this.clientContext = clientContext;
    }

    public Response get() throws RpcTimeoutException {
        long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        long requestId = request.getRequestId();
        ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            Response response = clientContext.fetchResponse(requestId);
            for (; response == null; response = clientContext.fetchResponse(requestId)) {
                if (nanos <= 0) {
                    throw new RpcTimeoutException("Rpc request timeout. request: `" + request + "`. timeout: `" + timeout + "`.");
                }
                nanos = done.awaitNanos(nanos);
            }
            return response;
        } catch (InterruptedException e) {
            // should not happen
            throw new RpcTimeoutException("Rpc request timeout: `" + e.getMessage() + "`. request: `" + request + "`. timeout: `" + timeout + "`.");
        } finally {
            lock.unlock();
        }
    }

}
