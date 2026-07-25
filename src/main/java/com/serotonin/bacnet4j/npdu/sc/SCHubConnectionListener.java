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

package com.serotonin.bacnet4j.npdu.sc;

import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;

/**
 * Listener for changes to a BACnet/SC network's hub connector state. Unlike other network types, an SC
 * network is not usable when {@code LocalDevice.initialize()} returns: the hub connection is established
 * asynchronously, and until then outgoing messages are dropped. Register a listener with
 * {@link SCNetwork#addHubConnectionListener} to learn when the network becomes usable
 * ({@code noHubConnection} to {@code connectedToPrimary} or {@code connectedToFailover}), when it fails
 * over or recovers between hubs, and when the connection is lost.
 *
 * @see SCNetwork#whenHubConnected()
 */
@FunctionalInterface
public interface SCHubConnectionListener {
    /**
     * Called when the hub connector state changes. Notified on the local device's executor while the hub
     * connector processes its state machine, so implementations must return quickly and must not block;
     * hand off substantial work to another thread.
     *
     * @param oldState the state before the change.
     * @param newState the state after the change.
     */
    void hubConnectionStateChanged(SCHubConnectorState oldState, SCHubConnectorState newState);
}
