package com.neil.simplerpc.core.client;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 请求 ID 生产器，根据 AtomicLong 自增长产生
 *
 * @author neil
 */
public class RequestIdGenerator {

    /**
     * 自增长 ID
     */
    private AtomicLong id = new AtomicLong();

    /**
     * 获取新的请求 ID
     *
     * @return 请求 ID
     */
    public long get() {
        return id.incrementAndGet();
    }

}
