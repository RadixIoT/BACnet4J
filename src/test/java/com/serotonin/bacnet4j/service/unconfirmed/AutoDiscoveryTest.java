package com.serotonin.bacnet4j.service.unconfirmed;

import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;

public class AutoDiscoveryTest {
    /**
     * Test to ensure that when an IHave is received from an unknown device that a properly configured RemoteDevice
     * object gets cached.
     *
     * @throws Exception
     */
    @Test
    public void iHaveToWhoIs() throws Exception {
        final TestNetworkMap map = new TestNetworkMap();
        try (final LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
                final LocalDevice d2 = new LocalDevice(2,
                        new DefaultTransport(new TestNetwork(map, 2, 0))).initialize()) {
            d1.getEventHandler().addListener(new DeviceEventAdapter() {
                @Override
                public void iHaveReceived(final RemoteDevice d, final RemoteObject o) {
                    assertEquals(Segmentation.segmentedBoth,
                            d.getDeviceProperty(PropertyIdentifier.segmentationSupported));
                }
            });

            // Send an IHave from d2
            d2.sendGlobalBroadcast(new IHaveRequest(d2.getId(), d2.getId(), d2.get(PropertyIdentifier.objectName)));

            // Wait while d1 receives the IHave, sends a WhoIs to d2, and then receives an IAm from d2 and creates
            // a remote device from the content.
            awaitTrue(() -> d1.getCachedRemoteDevice(2) != null);

            // Check a property that is not in the IHave, but is in the IAm
            assertEquals(Segmentation.segmentedBoth,
                    d1.getCachedRemoteDevice(2).getDeviceProperty(PropertyIdentifier.segmentationSupported));
        }
    }
}
