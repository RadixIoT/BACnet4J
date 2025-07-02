/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Radix IoT LLC,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.radixiot.com for commercial license options.
 */

package com.serotonin.bacnet4j.npdu.test;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.serotonin.bacnet4j.type.constructed.Address;

public class TestNetworkMap implements Iterable<TestNetwork> {
    private final Map<Address, TestNetwork> instances = new ConcurrentHashMap<>();

    public void add(final Address address, final TestNetwork network) {
        if (instances.containsKey(address))
            throw new IllegalStateException("Network map already contains key " + address);
        instances.put(address, network);
    }

    public void remove(final Address address) {
        if (!instances.containsKey(address))
            throw new IllegalStateException("Network map does not contain key " + address);
        instances.remove(address);
    }

    public TestNetwork get(final Address address) {
        return instances.get(address);
    }

    @Override
    public Iterator<TestNetwork> iterator() {
        return instances.values().iterator();
    }
}
