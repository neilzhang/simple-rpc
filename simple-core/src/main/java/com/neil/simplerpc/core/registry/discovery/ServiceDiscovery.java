package com.neil.simplerpc.core.registry.discovery;

import com.neil.simplerpc.core.service.ServiceDescriptor;
import com.neil.simplerpc.core.service.ServiceInstance;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author neil
 */
public class ServiceDiscovery {

    private static final String ROOT_PATH = "/simple-rpc";

    private CuratorFramework zkClient;

    private ExecutorService executor = Executors.newCachedThreadPool();

    private List<PathChildrenCache> pathChildrenCacheList = new ArrayList<>();

    public ServiceDiscovery(String zkConn) {
        this.zkClient = CuratorFrameworkFactory.newClient(zkConn, new ExponentialBackoffRetry(1000, 3));
    }

    public void start() {
        this.zkClient.start();
    }

    public void close() {
        for (PathChildrenCache childrenCache : pathChildrenCacheList) {
            try {
                childrenCache.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();
        this.zkClient.close();
    }

    public void subscribe(final ServiceDescriptor descriptor, ServiceListener listener) {
        String servicePath = ZKPaths.makePath(ROOT_PATH, descriptor.getService());
        PathChildrenCache childrenCache = new PathChildrenCache(this.zkClient, servicePath, true);
        childrenCache.getListenable().addListener(
                new PathChildrenCacheListener() {
                    @Override
                    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                        String target = new String(event.getData().getData());
                        ServiceInstance serviceInstance = createServiceInstance(descriptor, target);
                        switch (event.getType()) {
                            case CHILD_ADDED:
                                listener.onAdd(serviceInstance);
                                break;
                            case CHILD_REMOVED:
                                listener.onRemove(serviceInstance);
                                break;
                            default:
                                break;
                        }
                    }
                }, this.executor
        );
        try {
            childrenCache.start();
            this.pathChildrenCacheList.add(childrenCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ServiceInstance createServiceInstance(ServiceDescriptor descriptor, String target) {
        try {
            String[] parts = target.split(":");
            String host = parts[0];
            int port = Integer.valueOf(parts[1]);
            return new ServiceInstance(descriptor, host, port);
        } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }

}
