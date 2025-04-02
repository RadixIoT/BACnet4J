package com.serotonin.bacnet4j.npdu.test;

import com.serotonin.bacnet4j.type.constructed.Address;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TestNetworkMap<T extends AbstractTestNetwork> implements Iterable<T> {
    private final Map<Address, T> instances = new ConcurrentHashMap<>();

    public void add(final Address address, final T network) {
        if (instances.containsKey(address))
            throw new IllegalStateException("Network map already contains key " + address);
        instances.put(address, network);
    }

    public void remove(final Address address) {
        if (!instances.containsKey(address))
            throw new IllegalStateException("Network map does not contain key " + address);
        instances.remove(address);
    }

    public T get(final Address address) {
        return instances.get(address);
    }

    @Override
    public Iterator<T> iterator() {
        return instances.values().iterator();
    }
}
