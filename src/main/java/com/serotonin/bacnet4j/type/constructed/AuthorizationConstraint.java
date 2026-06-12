/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetErrorException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Enumerated;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationConstraint extends BaseType {
    private final Origin origin;
    private final Authentication authentication;

    public AuthorizationConstraint(Origin origin, Authentication authentication) {
        this.origin = origin;
        this.authentication = authentication;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, origin);
        write(queue, authentication);
    }

    @Override
    public String toString() {
        return "AuthorizationConstraint [" +
                "origin=" + origin +
                ", authentication=" + authentication +
                ']';
    }

    public Origin getOrigin() {
        return origin;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public AuthorizationConstraint(final ByteQueue queue) throws BACnetException {
        origin = read(queue, Origin.class);
        authentication = read(queue, Authentication.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationConstraint that = (AuthorizationConstraint) o;
        return Objects.equals(origin, that.origin) && Objects.equals(authentication,
                that.authentication);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, authentication);
    }

    public static class Origin extends Enumerated {
        public static final Origin directConnect = new Origin(0);
        public static final Origin sameNetwork = new Origin(1);
        public static final Origin anyNetwork = new Origin(2);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static Origin forId(final int id) {
            Origin e = (Origin) idMap.get(id);
            if (e == null)
                e = new Origin(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static Origin forName(final String name) {
            return (Origin) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private Origin(final int value) {
            super(value);
        }

        public Origin(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        /**
         * Returns a unmodifiable map.
         *
         * @return unmodifiable map
         */
        public static Map<Integer, String> getPrettyMap() {
            return Collections.unmodifiableMap(prettyMap);
        }

        /**
         * Returns a unmodifiable nameMap.
         *
         * @return unmodifiable map
         */
        public static Map<String, Enumerated> getNameMap() {
            return Collections.unmodifiableMap(nameMap);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }


    public static class Authentication extends Enumerated {
        public static final Authentication certified = new Authentication(0);
        public static final Authentication securePath = new Authentication(1);
        public static final Authentication anyMethod = new Authentication(2);

        private static final Map<Integer, Enumerated> idMap = new HashMap<>();
        private static final Map<String, Enumerated> nameMap = new HashMap<>();
        private static final Map<Integer, String> prettyMap = new HashMap<>();

        static {
            Enumerated.init(MethodHandles.lookup().lookupClass(), idMap, nameMap, prettyMap);
        }

        public static Authentication forId(final int id) {
            Authentication e = (Authentication) idMap.get(id);
            if (e == null)
                e = new Authentication(id);
            return e;
        }

        public static String nameForId(final int id) {
            return prettyMap.get(id);
        }

        public static Authentication forName(final String name) {
            return (Authentication) Enumerated.forName(nameMap, name);
        }

        public static int size() {
            return idMap.size();
        }

        private Authentication(final int value) {
            super(value);
        }

        public Authentication(final ByteQueue queue) throws BACnetErrorException {
            super(queue);
        }

        /**
         * Returns a unmodifiable map.
         *
         * @return unmodifiable map
         */
        public static Map<Integer, String> getPrettyMap() {
            return Collections.unmodifiableMap(prettyMap);
        }

        /**
         * Returns a unmodifiable nameMap.
         *
         * @return unmodifiable map
         */
        public static Map<String, Enumerated> getNameMap() {
            return Collections.unmodifiableMap(nameMap);
        }

        @Override
        public String toString() {
            return super.toString(prettyMap);
        }
    }
}
