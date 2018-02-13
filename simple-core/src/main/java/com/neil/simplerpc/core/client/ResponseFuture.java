package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
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

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition done = lock.newCondition();

    public ResponseFuture(Request request, long timeout) {
        this.request = request;
        this.timeout = timeout;
    }

    public Response get() throws RpcTimeoutException {
        long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        long requestId = request.getRequestId();
        ReentrantLock lock = this.lock;
        try {
            lock.lockInterruptibly();
            Response response = ResponseShelf.fetch(requestId);
            for (; response == null; response = ResponseShelf.fetch(requestId)) {
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
