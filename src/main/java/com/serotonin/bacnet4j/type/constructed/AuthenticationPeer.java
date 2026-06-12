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

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthenticationPeer extends BaseType {
    private final HostNPort host;
    private final Unsigned32 device;
    private final Boolean authAware;
    private final Boolean router;
    private final Boolean hub;

    public AuthenticationPeer(
            HostNPort host,
            Unsigned32 device,
            Boolean authAware,
            Boolean router,
            Boolean hub) {
        this.host = host;
        this.device = device;
        this.authAware = authAware;
        this.router = router;
        this.hub = hub;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, host);
        write(queue, device);
        write(queue, authAware);
        write(queue, router);
        write(queue, hub);
    }

    @Override
    public String toString() {
        return "AuthenticationPeer [" +
                "host=" + host +
                ", device=" + device +
                ", authAware=" + authAware +
                ", router=" + router +
                ", hub=" + hub +
                ']';
    }

    public HostNPort getHost() {
        return host;
    }

    public Unsigned32 getDevice() {
        return device;
    }

    public Boolean getAuthAware() {
        return authAware;
    }

    public Boolean getRouter() {
        return router;
    }

    public Boolean getHub() {
        return hub;
    }

    public AuthenticationPeer(final ByteQueue queue) throws BACnetException {
        host = read(queue, HostNPort.class);
        device = read(queue, Unsigned32.class);
        authAware = read(queue, Boolean.class);
        router = read(queue, Boolean.class);
        hub = read(queue, Boolean.class);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthenticationPeer that = (AuthenticationPeer) o;
        return Objects.equals(host, that.host) && Objects.equals(device,
                that.device) && Objects.equals(authAware, that.authAware) && Objects.equals(router,
                that.router) && Objects.equals(hub, that.hub);
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, device, authAware, router, hub);
    }
}
