package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.Request;
import com.neil.simplerpc.core.Response;
import com.neil.simplerpc.core.exception.RpcTimeoutException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 用于延迟获取 Response 的数据结构
 *
 * @author neil
 */
public class ResponseFuture {

    private final Request request;

    private final long timeout;

    private final ClientContext clientContext;

    private final ReentrantLock lock;

    private final Condition done;

    public ResponseFuture(Request request, long timeout, ClientContext clientContext) {
        this.request = request;
        this.timeout = timeout;
        this.clientContext = clientContext;
        this.lock = new ReentrantLock();
        this.done = this.lock.newCondition();
    }

    /**
     * 获取 Response 结果。该方法为阻塞方法。该方法将阻塞直到服务端返回 Response。
     *
     * @return
     * @throws RpcTimeoutException 如果阻塞等待时间超过 timeout 设置，将抛出该异常
     */
    public Response get() throws RpcTimeoutException, InterruptedException {
        long nanos = TimeUnit.MILLISECONDS.toNanos(timeout);
        long requestId = request.getRequestId();
        ReentrantLock lock = this.lock;
        lock.lockInterruptibly();
        try {
            Response response = clientContext.getResponse(requestId);
            for (; response == null; response = clientContext.getResponse(requestId)) {
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

    /**
     * 发送接收到 Response 的信号
     */
    public void received() {
        ReentrantLock lock = this.lock;
        lock.lock();
        try {
            done.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

}
