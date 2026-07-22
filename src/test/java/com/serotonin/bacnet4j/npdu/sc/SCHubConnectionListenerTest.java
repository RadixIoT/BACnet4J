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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;

/**
 * Covers the hub connection listener management on SCNetwork: dispatch, removal, exception isolation,
 * and the whenHubConnected() future. The connector-side firing of these notifications is covered in
 * SCHubConnectorTest.
 */
public class SCHubConnectionListenerTest {
    private SCNetwork network;

    @Before
    public void setUp() {
        network = new SCNetworkBuilder()
                .vmac("010203040506")
                .uuid("46663baa-98cc-4cf7-ad19-503f4705b130")
                .primaryHubUri("wss://hub.example.com:4443")
                .keyPairHandler(new InMemoryKeyPairHandler(mock(KeyPair.class)))
                .operationalCertificateFileId(1)
                .issuerCertificateFile1Id(2)
                .issuerCertificateFile2Id(3)
                .certificateSigningRequestFileId(4)
                .build();
    }

    @Test
    public void listenersReceiveStateChanges() {
        List<SCHubConnectorState> received = new ArrayList<>();
        network.addHubConnectionListener((oldState, newState) -> received.add(newState));

        network.fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);
        network.fireHubConnectionStateChanged(
                SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection);

        assertEquals(List.of(SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection),
                received);
    }

    @Test
    public void removedListenerIsNotNotified() {
        List<SCHubConnectorState> received = new ArrayList<>();
        SCHubConnectionListener listener = (oldState, newState) -> received.add(newState);
        network.addHubConnectionListener(listener);
        network.removeHubConnectionListener(listener);

        network.fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);

        assertTrue(received.isEmpty());
    }

    @Test
    public void listenerExceptionDoesNotAffectOtherListeners() {
        List<SCHubConnectorState> received = new ArrayList<>();
        network.addHubConnectionListener((oldState, newState) -> {
            throw new RuntimeException("listener failure");
        });
        network.addHubConnectionListener((oldState, newState) -> received.add(newState));

        network.fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);

        assertEquals(List.of(SCHubConnectorState.connectedToPrimary), received);
    }

    @Test
    public void whenHubConnectedCompletesOnConnection() {
        var future = network.whenHubConnected();
        assertFalse(future.isDone());

        network.fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToFailover);

        assertTrue(future.isDone());
        assertEquals(SCHubConnectorState.connectedToFailover, future.join());
    }

    @Test
    public void whenHubConnectedIgnoresDisconnection() {
        var future = network.whenHubConnected();

        network.fireHubConnectionStateChanged(
                SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection);

        assertFalse(future.isDone());
    }

    @Test
    public void isHubConnectedReflectsConnectorState() {
        // Uninitialized network has no node, which reads as no hub connection.
        assertFalse(network.isHubConnected());
    }
}
