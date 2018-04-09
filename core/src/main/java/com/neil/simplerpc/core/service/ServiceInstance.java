package com.neil.simplerpc.core.service;

import java.util.Objects;

/**
 * @author neil
 */
public class ServiceInstance {

    private final ServiceDescriptor descriptor;

    private final String host;

    private final int port;

    public ServiceInstance(ServiceDescriptor descriptor, String host, int port) {
        this.descriptor = descriptor;
        this.host = host;
        this.port = port;
    }

    public ServiceDescriptor getDescriptor() {
        return descriptor;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceInstance that = (ServiceInstance) o;
        return port == that.port &&
                Objects.equals(descriptor, that.descriptor) &&
                Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor, host, port);
    }

    @Override
    public String toString() {
        return "ServiceInstance{" +
                "descriptor=" + descriptor +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
