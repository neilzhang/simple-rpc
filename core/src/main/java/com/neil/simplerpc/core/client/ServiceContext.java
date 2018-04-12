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
public abstract class ServiceContext implements ServiceListener {

    private final ConcurrentHashMap<ServiceDescriptor, ServiceGroup> serviceGroupMap;

    private static final int STATE_CLOSED = -1;

    private static final int STATE_BUILD = 0;

    private volatile int state = STATE_BUILD;

    public ServiceContext() {
        this.serviceGroupMap = new ConcurrentHashMap<>();
    }

    public ServiceProxy getServiceProxy(ServiceDescriptor descriptor) {
        ServiceGroup serviceGroup = serviceGroupMap.get(descriptor);
        if (serviceGroup == null) {
            throw new SimpleRpcException("there is no available service provider for service descriptor `" + descriptor + "`.");
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

    @Override
    public void onAdd(ServiceInstance serviceInstance) {
        add(serviceInstance);
    }

    @Override
    public void onRemove(ServiceInstance serviceInstance) {
        remove(serviceInstance);
    }

    public void destroy() {
        if (state == STATE_BUILD) {
            state = STATE_CLOSED;
            for (ServiceGroup serviceGroup : serviceGroupMap.values()) {
                serviceGroup.close();
            }
        }
    }

    public abstract ServiceProxy createServiceProxy(ServiceInstance instance);

    class ServiceGroup {

        private final ExecutorService recoverService;

        private final ServiceDescriptor descriptor;

        private final AtomicLong count;

        private final List<ServiceProxy> proxyList;

        private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        private ConcurrentHashMap<ServiceInstance, Long> recoverMap;

        public ServiceGroup(ServiceDescriptor descriptor) {
            this.descriptor = descriptor;
            this.count = new AtomicLong();
            this.proxyList = new ArrayList<>();
            this.recoverMap = new ConcurrentHashMap<>();
            this.recoverService = Executors.newSingleThreadExecutor();
        }

        public ServiceProxy get() {
            List<ServiceProxy> copy = new ArrayList<>(proxyList);
            ServiceProxy proxy = null;
            int failCount = 0;
            while (!copy.isEmpty() && failCount < 3) {
                int index = (int) Math.abs(count.incrementAndGet() % copy.size());
                proxy = copy.get(index);
                if (proxy.isActive()) {
                    break;
                } else {
                    if (proxy.isConnectionBroken()) {
                        recoverService.submit(new RecoverTask(proxy.getServiceInstance()));
                    }
                    failCount++;
                }
            }
            return proxy;
        }

        public void add(ServiceInstance serviceInstance) {
            if (!serviceInstance.getDescriptor().equals(descriptor)) {
                return;
            }
            try {
                lock.writeLock().lock();
                ServiceProxy proxy = get(serviceInstance);
                if (proxy == null) {
                    proxyList.add(createServiceProxy(serviceInstance));
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
                ServiceProxy proxy = get(serviceInstance);
                if (proxy != null) {
                    proxy.close();
                    proxyList.remove(proxy);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        public void close() {
            try {
                lock.writeLock().lock();
                recoverService.shutdown();
                for (ServiceProxy proxy : proxyList) {
                    proxy.close();
                }
                proxyList.clear();
            } finally {
                lock.writeLock().unlock();
            }
        }

        private ServiceProxy get(ServiceInstance serviceInstance) {
            for (ServiceProxy proxy : proxyList) {
                if (serviceInstance.equals(proxy.getServiceInstance())) {
                    return proxy;
                }
            }
            return null;
        }

        class RecoverTask implements Runnable {

            private ServiceInstance serviceInstance;

            public RecoverTask(ServiceInstance serviceInstance) {
                this.serviceInstance = serviceInstance;
            }

            @Override
            public void run() {
                try {
                    Long lastRecoverTime = recoverMap.get(serviceInstance);
                    if (lastRecoverTime == null || System.currentTimeMillis() - lastRecoverTime > 3 * 1000) {
                        recoverMap.put(serviceInstance, System.currentTimeMillis());

                        remove(serviceInstance);
                        add(serviceInstance);
                    }
                } catch (Exception e) {
                    e.printStackTrace();// TODO
                }
            }

        }

    }

}
