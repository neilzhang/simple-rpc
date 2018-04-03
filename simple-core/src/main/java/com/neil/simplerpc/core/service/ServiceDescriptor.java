package com.neil.simplerpc.core.service;

import java.util.Objects;

/**
 * @author neil
 */
public class ServiceDescriptor {

    private final String service;

    public ServiceDescriptor(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceDescriptor that = (ServiceDescriptor) o;
        return Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service);
    }

    @Override
    public String toString() {
        return "ServiceDescriptor{" +
                "service='" + service + '\'' +
                '}';
    }
}
