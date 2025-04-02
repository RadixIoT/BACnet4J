/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.cache;

import com.serotonin.bacnet4j.LocalDevice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RemoteEntityCache<K, T> {
    static final Logger LOG = LoggerFactory.getLogger(RemoteEntityCache.class);

    /**
     * The local device, as usual.
     */
    private final LocalDevice localDevice;

    /**
     * The cache of entities, wrapped with their policy state.
     */
    private final Map<K, CachedRemoteEntity<T>> cache = new HashMap<>();

    public RemoteEntityCache(final LocalDevice localDevice) {
        this.localDevice = localDevice;
    }

    public T getCachedEntity(final K key) {
        // Check for a cached instance
        synchronized (cache) {
            final CachedRemoteEntity<T> cre = cache.get(key);
            if (cre != null && !cre.hasExpired(localDevice)) {
                LOG.debug("Returning cached entity: {}", key);
                return cre.getEntity();
            }

            if (cre != null) {
                // The value has expired. Remove it from the cache.
                cache.remove(key);
            }

            return null;
        }
    }

    public T getCachedEntity(final Predicate<T> predicate) {
        synchronized (cache) {
            final Iterator<CachedRemoteEntity<T>> iter = cache.values().iterator();
            while (iter.hasNext()) {
                final CachedRemoteEntity<T> cre = iter.next();
                if (cre.hasExpired(localDevice)) {
                    iter.remove();
                } else if (predicate.test(cre.getEntity())) {
                    return cre.getEntity();
                }
            }
            return null;
        }
    }

    public void putEntity(final K key, final T value, final RemoteEntityCachePolicy policy) {
        synchronized (cache) {
            cache.put(key, new CachedRemoteEntity<>(localDevice, value, policy));
        }
    }

    public T removeEntity(final K key) {
        synchronized (cache) {
            final CachedRemoteEntity<T> cp = cache.remove(key);
            if (cp == null)
                return null;
            return cp.getEntity();
        }
    }

    public List<T> getEntities() {
        synchronized (cache) {
            return cache.values().stream().map((e) -> e.getEntity()).collect(Collectors.toList());
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
    }
}
