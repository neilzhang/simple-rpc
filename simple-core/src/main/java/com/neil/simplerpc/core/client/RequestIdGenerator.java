package com.neil.simplerpc.core.client;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author neil
 */
public class RequestIdGenerator {

    private AtomicLong id = new AtomicLong();

    public long get() {
        return id.incrementAndGet();
    }

}
