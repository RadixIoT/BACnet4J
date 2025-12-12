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

package com.serotonin.bacnet4j.type.constructed;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.primitive.Primitive;

public class ChoiceOptions {
    private final List<Class<? extends Primitive>> primitives;
    private final Map<Integer, ContextualType> contextual;

    public ChoiceOptions() {
        this.primitives = new ArrayList<>();
        this.contextual = new HashMap<>();
    }

    public ChoiceOptions(List<Class<? extends Primitive>> primitives, Map<Integer, ContextualType> contextual) {
        this.primitives = primitives;
        this.contextual = contextual;
    }

    public List<Class<? extends Primitive>> getPrimitives() {
        return primitives;
    }

    public Map<Integer, ContextualType> getContextual() {
        return contextual;
    }

    public void addPrimitive(final Class<? extends Primitive> primitive) {
        primitives.add(primitive);
    }

    public void addContextual(final Integer contextId, final Class<? extends Encodable> clazz) {
        contextual.put(contextId, new ContextualType(clazz, false));
    }

    public void addContextualSequence(final Integer contextId, final Class<? extends Encodable> clazz) {
        contextual.put(contextId, new ContextualType(clazz, true));
    }

    public ContextualType getContextualClass(final int contextId) {
        return contextual.get(contextId);
    }

    public boolean containsPrimitive(final Class<? extends Primitive> clazz) {
        return primitives.contains(clazz);
    }

    public int getContextId(final Class<? extends Encodable> clazz, final boolean sequence) {
        for (final Map.Entry<Integer, ContextualType> e : contextual.entrySet()) {
            if (e.getValue().clazz.equals(clazz) && e.getValue().sequence == sequence)
                return e.getKey();
        }
        return -1;
    }

    public static class ContextualType {
        private final Class<? extends Encodable> clazz;
        private final boolean sequence;

        public ContextualType(final Class<? extends Encodable> clazz, final boolean sequence) {
            this.clazz = clazz;
            this.sequence = sequence;
        }

        public Class<? extends Encodable> getClazz() {
            return clazz;
        }

        public boolean isSequence() {
            return sequence;
        }
    }
}
