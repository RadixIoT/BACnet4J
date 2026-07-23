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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.obj.FileObject;
import com.serotonin.bacnet4j.obj.SecureConnectNetworkPortObject;
import com.serotonin.bacnet4j.obj.fileAccess.FileStreamAccess;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SCHubConnectorState;
import com.serotonin.bacnet4j.type.primitive.OctetString;

/**
 * Covers the hub connection listener management on SCNetwork: dispatch, removal, exception isolation,
 * and the whenHubConnected() future. The connector-side firing of these notifications is covered in
 * SCHubConnectorTest.
 */
public class SCHubConnectionListenerTest {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private SCNetwork network;
    private InMemoryKeyPairHandler keyPairHandler;

    @Before
    public void setUp() {
        keyPairHandler = new InMemoryKeyPairHandler(mock(KeyPair.class));
        network = new SCNetworkBuilder()
                .vmac("010203040506")
                .uuid("46663baa-98cc-4cf7-ad19-503f4705b130")
                .primaryHubUri("wss://hub.example.com:4443")
                .keyPairHandler(keyPairHandler)
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

    /**
     * Terminating the network cancels pending whenHubConnected() futures so that blocked callers are
     * released, and cancellation removes the future's listener so later state changes do not attempt
     * to complete a canceled future.
     */
    @Test
    public void whenHubConnectedIsCancelledOnTerminate() {
        var future = network.whenHubConnected();

        network.terminate();

        assertTrue(future.isCancelled());

        // A subsequent connection does not resurrect the future.
        network.fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);
        assertTrue(future.isCancelled());
    }

    /**
     * A network that never initialized has no node and therefore nothing to shut down, so
     * awaitTermination returns immediately.
     */
    @Test
    public void awaitTerminationReturnsImmediatelyWhenNotInitialized() throws Exception {
        network.terminate();

        assertTrue(network.awaitTermination(0, TimeUnit.MILLISECONDS));
    }

    @Test
    public void completedFutureIsUnaffectedByTerminate() {
        var future = network.whenHubConnected();
        network.fireHubConnectionStateChanged(
                SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);
        assertEquals(SCHubConnectorState.connectedToPrimary, future.join());

        network.terminate();

        assertFalse(future.isCancelled());
        assertEquals(SCHubConnectorState.connectedToPrimary, future.join());
    }

    /**
     * The SC network port object subscribes to hub connection state changes and keeps
     * SC_Hub_Connector_State and the hub connection status properties current. When the object is
     * removed from the device (which terminates it), its listener is removed, so further changes do
     * not update the terminated object.
     */
    private SecureConnectNetworkPortObject addPortObject(LocalDevice localDevice) throws Exception {
        localDevice.addObject(new FileObject(localDevice, 1, "pem",
                new FileStreamAccess(tempFolder.newFile("operational.pem"))));
        localDevice.addObject(new FileObject(localDevice, 2, "pem",
                new FileStreamAccess(tempFolder.newFile("issuer1.pem"))));
        localDevice.addObject(new FileObject(localDevice, 3, "pem",
                new FileStreamAccess(tempFolder.newFile("issuer2.pem"))));
        return localDevice.addObject(new SecureConnectNetworkPortObject(
                localDevice, network, keyPairHandler, 12));
    }

    @Test
    public void portObjectPropertiesFollowConnectorState() throws Exception {
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network))) {
            var npo = addPortObject(localDevice);
            localDevice.initialize();

            assertEquals(SCHubConnectorState.noHubConnection,
                    npo.readProperty(PropertyIdentifier.scHubConnectorState));

            network.fireHubConnectionStateChanged(
                    SCHubConnectorState.noHubConnection, SCHubConnectorState.connectedToPrimary);

            assertEquals(SCHubConnectorState.connectedToPrimary,
                    npo.readProperty(PropertyIdentifier.scHubConnectorState));
            assertNotNull(npo.readProperty(PropertyIdentifier.scPrimaryHubConnectionStatus));
            assertNotNull(npo.readProperty(PropertyIdentifier.scFailoverHubConnectionStatus));

            // Removing the object terminates it, which removes its listener from the network.
            localDevice.removeObject(npo.getId());
            network.fireHubConnectionStateChanged(
                    SCHubConnectorState.connectedToPrimary, SCHubConnectorState.noHubConnection);
            assertEquals(SCHubConnectorState.connectedToPrimary,
                    npo.get(PropertyIdentifier.scHubConnectorState));
        }
    }

    /**
     * The node re-randomizes its VMAC when the hub reports a duplicate, so the port object refreshes
     * MAC_Address from the network on read.
     */
    @Test
    public void macAddressRefreshesOnRead() throws Exception {
        try (var localDevice = new LocalDevice(1, new DefaultTransport(network))) {
            var npo = addPortObject(localDevice);
            localDevice.initialize();

            assertEquals(OctetString.fromHex("010203040506"),
                    npo.readProperty(PropertyIdentifier.macAddress));

            // Simulate the duplicate-VMAC re-randomization.
            network.setVmac(OctetString.fromHex("665544332211"));

            assertEquals(OctetString.fromHex("665544332211"),
                    npo.readProperty(PropertyIdentifier.macAddress));
        }
    }
}
