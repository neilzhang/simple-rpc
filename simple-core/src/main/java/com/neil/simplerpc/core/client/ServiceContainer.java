package com.neil.simplerpc.core.client;

import com.neil.simplerpc.core.exception.SimpleRpcException;
import com.neil.simplerpc.core.registry.discovery.ServiceListener;
import com.neil.simplerpc.core.service.ServiceDescriptor;
import com.neil.simplerpc.core.service.ServiceInstance;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author neil
 */
public class ServiceContainer implements ServiceListener {

    private final ClientContext clientContext;

    private final ExecutorService recoverService;

    private final ConcurrentHashMap<ServiceDescriptor, ServiceGroup> serviceGroupMap;

    private static final int STATE_CLOSED = -1;

    private static final int STATE_BUILD = 0;

    private volatile int state = STATE_BUILD;

    public ServiceContainer(ClientContext clientContext) {
        this.clientContext = clientContext;
        this.recoverService = Executors.newSingleThreadExecutor();
        this.serviceGroupMap = new ConcurrentHashMap<>();
    }

    public ServiceProxy getServiceProxy(ServiceDescriptor descriptor) {
        ServiceGroup serviceGroup = serviceGroupMap.get(descriptor);
        if (serviceGroup == null) {
            throw new SimpleRpcException("there are no available service proxies. service descriptor: `" + descriptor + "`.");
        }
        return serviceGroup.get();
    }

    public void add(ServiceInstance serviceInstance) {
        if (state == STATE_CLOSED) {
            throw new SimpleRpcException("service container is already closed. add service instance: `" + serviceInstance + "`.");
        }
        ServiceDescriptor descriptor = serviceInstance.getDescriptor();
        ServiceGroup serviceGroup = serviceGroupMap.get(descriptor);
        if (serviceGroup == null) {
            synchronized (serviceGroupMap) {
                serviceGroup = serviceGroupMap.get(descriptor);
                if (serviceGroup == null) {
                    serviceGroup = new ServiceGroup(descriptor);
                    serviceGroupMap.put(descriptor, serviceGroup);
                }
            }
        }
        serviceGroup.add(serviceInstance);
    }

    public void remove(ServiceInstance serviceInstance) {
        if (state == STATE_CLOSED) {
            throw new SimpleRpcException("service container is already closed. remove service instance: `" + serviceInstance + "`.");
        }
        ServiceDescriptor descriptor = serviceInstance.getDescriptor();
        ServiceGroup serviceGroup = serviceGroupMap.get(descriptor);
        if (serviceGroup != null) {
            serviceGroup.remove(serviceInstance);
        }
    }

    public void recover(ServiceProxy proxy) {
        if (state == STATE_CLOSED) {
            throw new SimpleRpcException("service container is already closed. recover service instance: `" + proxy + "`.");
        }
        recoverService.submit(new RecoverTask(proxy));
    }

    @Override
    public void onAdd(ServiceInstance serviceInstance) {
        add(serviceInstance);
    }

    @Override
    public void onRemove(ServiceInstance serviceInstance) {
        remove(serviceInstance);
    }

    public void close() {
        if (state == STATE_BUILD) {
            state = STATE_CLOSED;
            recoverService.shutdown();
            for (ServiceGroup serviceGroup : serviceGroupMap.values()) {
                serviceGroup.clear();
            }
        }
    }

    class ServiceGroup {

        private final ServiceDescriptor descriptor;

        private final AtomicLong count;

        private final List<ServiceProxy> proxyList;

        private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        public ServiceGroup(ServiceDescriptor descriptor) {
            this.descriptor = descriptor;
            this.count = new AtomicLong();
            this.proxyList = new ArrayList<>();
        }

        public ServiceProxy get() {
            ServiceProxy proxy = null;
            int failCount = 0;
            try {
                lock.readLock().lock();
                while (!proxyList.isEmpty() && failCount < 3) {
                    int index = (int) Math.abs(count.incrementAndGet() % proxyList.size());
                    proxy = proxyList.get(index);
                    if (proxy.isActive()) {
                        break;
                    } else {
                        if (proxy.isConnectionBroken()) {
                            recover(proxy);
                        }
                        failCount++;
                    }
                }
            } finally {
                lock.readLock().unlock();
            }
            return proxy;
        }

        public void add(ServiceInstance serviceInstance) {
            if (!serviceInstance.getDescriptor().equals(descriptor)) {
                return;
            }
            try {
                lock.writeLock().lock();
                if (!proxyList.contains(serviceInstance)) {
                    ServiceProxy serviceProxy = new ServiceProxy(
                            this.descriptor,
                            serviceInstance.getHost(),
                            serviceInstance.getPort(),
                            ServiceContainer.this.clientContext
                    );
                    proxyList.add(serviceProxy);
                }
            } finally {
                lock.writeLock().unlock();
            }

        }

        public void remove(ServiceInstance serviceInstance) {
            if (!serviceInstance.getDescriptor().equals(descriptor)) {
                return;
            }
            try {
                lock.writeLock().lock();
                int index = proxyList.indexOf(serviceInstance);
                if (index > 0) {
                    proxyList.get(index).close();
                    proxyList.remove(index);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void clear() {
            try {
                lock.writeLock().lock();
                for (ServiceProxy proxy : proxyList) {
                    proxy.close();
                }
                proxyList.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

    }

    class RecoverTask implements Runnable {

        private ServiceProxy proxy;

        public RecoverTask(ServiceProxy proxy) {
            this.proxy = proxy;
        }

        @Override
        public void run() {
            try {
                remove(proxy);
                add(proxy);
            } catch (Exception e) {
                e.printStackTrace();// TODO
            }
        }

    }

}
