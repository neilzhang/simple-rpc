package com.neil.simplerpc.core.registry.provider;

import com.neil.simplerpc.core.service.ServiceDescriptor;
import com.neil.simplerpc.core.service.ServiceInstance;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;

/**
 * @author neil
 */
public class ServiceProvider {

    private static final String ROOT_PATH = "/simple-rpc";

    private CuratorFramework zkClient;

    public ServiceProvider(String zkConn) {
        this.zkClient = CuratorFrameworkFactory.newClient(zkConn, new ExponentialBackoffRetry(1000, 3));
    }

    public void start() {
        this.zkClient.start();
    }

    public void close() {
        this.zkClient.close();
    }

    public void publish(ServiceInstance instance) {
        ServiceDescriptor descriptor = instance.getDescriptor();
        String target = getTarget(instance.getHost(), instance.getPort());
        String servicePath = ZKPaths.makePath(ROOT_PATH, descriptor.getService());
        String instancePath = ZKPaths.makePath(servicePath, target);
        try {
            zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(instancePath);
            zkClient.setData().forPath(instancePath, target.getBytes());
        } catch (Exception e) {
            // TODO
            e.printStackTrace();
        }
    }

    private String getTarget(String host, int port) {
        return host + ":" + port;
    }
}
