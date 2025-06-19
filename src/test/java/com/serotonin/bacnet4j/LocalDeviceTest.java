package com.serotonin.bacnet4j;

import static com.serotonin.bacnet4j.TestUtils.awaitTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Clock;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice.CacheUpdate;
import com.serotonin.bacnet4j.cache.CachePolicies;
import com.serotonin.bacnet4j.cache.RemoteEntityCachePolicy;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.exception.BACnetTimeoutException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.DeviceObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.DiscoveryUtils;
import com.serotonin.bacnet4j.util.RemoteDeviceDiscoverer;
import com.serotonin.bacnet4j.util.RemoteDeviceFinder.RemoteDeviceFuture;

import lohbihler.warp.WarpClock;

public class LocalDeviceTest {
    static final Logger LOG = LoggerFactory.getLogger(LocalDeviceTest.class);

    // The clock will control the expiration of devices from the cache, but not the real time delays
    // when doing discoveries.
    private final WarpClock clock = new WarpClock();
    private final TestNetworkMap map = new TestNetworkMap();
    LocalDevice d1;
    LocalDevice d2;

    @Before
    public void before() throws Exception {
        d1 = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 100))).initialize();
        d2 = new LocalDevice(2, new DefaultTransport(new TestNetwork(map, 2, 100))).initialize();
    }

    @After
    public void after() {
        // Shut down
        d1.terminate();
        d2.terminate();
    }

    @Test
    public void deviceCacheSuccess() throws InterruptedException, ExecutionException, BACnetException {
        assertNull(d1.getCachedRemoteDevice(2));

        RemoteDevice rd2 = d1.getRemoteDeviceBlocking(2);
        DiscoveryUtils.getExtendedDeviceInformation(d1, rd2);

        // Ask for device 2 in two different threads.
        final MutableObject<RemoteDevice> rd21 = new MutableObject<>();
        final MutableObject<RemoteDevice> rd22 = new MutableObject<>();
        final Future<?> future1 = d1.submit(() -> {
            try {
                rd21.setValue(d1.getRemoteDevice(2).get());
            } catch (final BACnetException e) {
                fail(e.getMessage());
                LOG.error("Should not have happened", e);
            }
        });
        final Future<?> future2 = d1.submit(() -> {
            try {
                rd22.setValue(d1.getRemoteDevice(2).get());
            } catch (final BACnetException e) {
                LOG.error("Should not have happened", e);
            }
        });

        future1.get();
        future2.get();

        assertSame(rd21.getValue(), rd22.getValue());
        assertNotNull(rd21.getValue().getDeviceProperty(PropertyIdentifier.protocolServicesSupported));
        assertNotNull(rd21.getValue().getDeviceProperty(PropertyIdentifier.objectName));
        assertNotNull(rd21.getValue().getDeviceProperty(PropertyIdentifier.protocolVersion));
        assertNotNull(rd21.getValue().getDeviceProperty(PropertyIdentifier.vendorIdentifier));
        assertNotNull(rd21.getValue().getDeviceProperty(PropertyIdentifier.modelName));

        // Ask for it again. Should be the same instance.
        final RemoteDevice rd23 = d1.getRemoteDevice(2).get();

        // Device is cached, so it will still be the same instance.
        assertSame(rd21.getValue(), rd23);
    }

    @Test(expected = BACnetTimeoutException.class)
    public void deviceCacheFailure() throws BACnetException {
        d1.getRemoteDevice(4).get(200);
    }

    @Test(expected = CancellationException.class)
    public void cancelGetRemoteDevice() throws CancellationException, BACnetException {
        final RemoteDeviceFuture future = d1.getRemoteDevice(3);
        future.cancel();
        future.get();
    }

    @Test
    public void undefinedDeviceId() throws Exception {
        try (final LocalDevice ld = new LocalDevice(ObjectIdentifier.UNINITIALIZED,
                new DefaultTransport(new TestNetwork(map, 3, 10)))) {
            ld.setClock(clock);
            new Thread(() -> clock.plus(200, TimeUnit.SECONDS, 10, TimeUnit.SECONDS, 10, 0)).start();
            ld.initialize();

            LOG.info("Local device initialized with device id {}", ld.getInstanceNumber());
            assertNotEquals(ObjectIdentifier.UNINITIALIZED, ld.getInstanceNumber());
        }
    }

    @Test
    public void getRemoteDeviceWithCallback() throws Exception {
        assertNull(d1.getCachedRemoteDevice(2));

        // Ask for device 2 in a different thread.
        final MutableObject<RemoteDevice> rd21 = new MutableObject<>();
        d1.getRemoteDevice(2, rd21::setValue, null, null, 1, TimeUnit.SECONDS);

        awaitTrue(() -> rd21.getValue() != null);
        assertSame(rd21.getValue(), d1.getCachedRemoteDevice(2));
    }

    @Test(expected = BACnetServiceException.class)
    public void createSecondDevice() throws BACnetServiceException {
        final LocalDevice ld = new LocalDevice(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        final DeviceObject o = new DeviceObject(ld, 2);

        // Ensure the device object was not automatically added to the local device.
        assertEquals(1, ld.getLocalObjects().size());

        // Try to add the device manually, and ensure that this fails.
        ld.addObject(o);
    }

    @SuppressWarnings("unused")
    @Test
    public void getDeviceBlockingTimeout() throws Exception {
        try (final LocalDevice d3 = new LocalDevice(3, new DefaultTransport(new TestNetwork(map, 3, 0))).withClock(
                clock).initialize()) {
            final long start = Clock.systemUTC().millis();

            try {
                d3.getRemoteDeviceBlocking(4, 100);
                fail();
            } catch (final BACnetTimeoutException e) {
                // Expected after 100ms.
                assertTrue(Clock.systemUTC().millis() - start >= 100);
            }

            try {
                d3.getRemoteDeviceBlocking(4, 1000);
                fail();
            } catch (final BACnetTimeoutException e) {
                // Expected immediately.
                assertTrue(Clock.systemUTC().millis() - start < 1000);
            }

            clock.plusMillis(30000);

            try {
                d3.getRemoteDeviceBlocking(4, 100);
                fail();
            } catch (final BACnetTimeoutException e) {
                // Expected after 100ms.
                assertTrue(Clock.systemUTC().millis() - start >= 100);
            }
        }
    }

    @Test
    public void remoteDeviceDiscoveryUpdateAlways() throws Exception {
        BlockingQueue<RemoteDevice> results = new ArrayBlockingQueue<>(2);

        try (LocalDevice localDevice = new LocalDevice(3, new DefaultTransport(new TestNetwork(map, 3, 0)))) {
            localDevice.setClock(clock);
            localDevice.initialize();

            CachePolicies cachePolicies = localDevice.getCachePolicies();
            cachePolicies.putDevicePolicy(1, RemoteEntityCachePolicy.EXPIRE_5_SECONDS);
            cachePolicies.putDevicePolicy(2, RemoteEntityCachePolicy.EXPIRE_1_MINUTE);

            try (RemoteDeviceDiscoverer discoverer = localDevice.startRemoteDeviceDiscovery(CacheUpdate.ALWAYS,
                    results::add)) {

                RemoteDevice response1 = poll(results);
                RemoteDevice response2 = poll(results);
                containsRemoteDeviceOneAndTwo(discoverer, response1, response2);

                // skip ahead 10 seconds, d1 should now be expired, d2 should not be expired
                clock.plusSeconds(10);
                d1.sendGlobalBroadcast(d1.getIAm());
                d2.sendGlobalBroadcast(d2.getIAm());

                // both d1 and d2 should have been updated - callback should fire 2x, and both should be in the latest devices list
                RemoteDevice response3 = poll(results);
                RemoteDevice response4 = poll(results);
                containsRemoteDeviceOneAndTwo(discoverer, response3, response4);
            }
        }

        // ensure no more iAm messages were received
        assertTrue(results.isEmpty());
    }

    @Test
    public void remoteDeviceDiscoveryUpdateIfExpired() throws Exception {
        BlockingQueue<RemoteDevice> results = new ArrayBlockingQueue<>(2);

        try (LocalDevice localDevice = new LocalDevice(3, new DefaultTransport(new TestNetwork(map, 3, 0)))) {
            localDevice.setClock(clock);
            localDevice.initialize();

            CachePolicies cachePolicies = localDevice.getCachePolicies();
            cachePolicies.putDevicePolicy(1, RemoteEntityCachePolicy.EXPIRE_5_SECONDS);
            cachePolicies.putDevicePolicy(2, RemoteEntityCachePolicy.EXPIRE_1_MINUTE);

            try (RemoteDeviceDiscoverer discoverer = localDevice.startRemoteDeviceDiscovery(CacheUpdate.IF_EXPIRED,
                    results::add)) {

                RemoteDevice response1 = poll(results);
                RemoteDevice response2 = poll(results);
                containsRemoteDeviceOneAndTwo(discoverer, response1, response2);

                // skip ahead 10 seconds, d1 should now be expired, d2 should not be expired
                clock.plusSeconds(10);
                d1.sendGlobalBroadcast(d1.getIAm());
                d2.sendGlobalBroadcast(d2.getIAm());

                // d1 should have been updated - callback should fire, and it should be in the latest devices list
                RemoteDevice response3 = poll(results);
                assertNotNull(response3);
                assertEquals(1, response3.getInstanceNumber());
                assertEquals(2, discoverer.getRemoteDevices().size());
                assertContainsExactlyInAnyOrder(discoverer.getLatestRemoteDevices(), response3);
            }
        }

        // ensure no more iAm messages were received
        assertTrue(results.isEmpty());
    }

    @Test
    public void remoteDeviceDiscoveryUpdateNever() throws Exception {
        BlockingQueue<RemoteDevice> results = new ArrayBlockingQueue<>(2);

        try (LocalDevice localDevice = new LocalDevice(3, new DefaultTransport(new TestNetwork(map, 3, 0)))) {
            localDevice.setClock(clock);
            localDevice.initialize();

            CachePolicies cachePolicies = localDevice.getCachePolicies();
            cachePolicies.putDevicePolicy(1, RemoteEntityCachePolicy.EXPIRE_5_SECONDS);
            cachePolicies.putDevicePolicy(2, RemoteEntityCachePolicy.EXPIRE_1_MINUTE);

            try (RemoteDeviceDiscoverer discoverer = localDevice.startRemoteDeviceDiscovery(CacheUpdate.NEVER,
                    results::add)) {

                RemoteDevice response1 = poll(results);
                RemoteDevice response2 = poll(results);
                containsRemoteDeviceOneAndTwo(discoverer, response1, response2);

                // skip ahead 10 seconds, d1 should now be expired, d2 should not be expired
                clock.plusSeconds(10);
                d1.sendGlobalBroadcast(d1.getIAm());
                d2.sendGlobalBroadcast(d2.getIAm());

                // neither should have been updated - no callback should fire, latest devices list should be empty
                RemoteDevice response3 = poll(results);
                assertNull(response3);
                assertEquals(2, discoverer.getRemoteDevices().size());
                assertEquals(0, discoverer.getLatestRemoteDevices().size());
            }
        }

        // ensure no more iAm messages were received
        assertTrue(results.isEmpty());
    }

    private RemoteDevice poll(BlockingQueue<RemoteDevice> results) throws InterruptedException {
        return results.poll(5, TimeUnit.SECONDS);
    }

    private void containsRemoteDeviceOneAndTwo(RemoteDeviceDiscoverer discoverer, RemoteDevice remoteDeviceA,
            RemoteDevice remoteDeviceB) {
        assertNotNull(remoteDeviceA);
        assertNotNull(remoteDeviceB);
        assertTrue(remoteDeviceA.getInstanceNumber() == 1 || remoteDeviceA.getInstanceNumber() == 2);
        if (remoteDeviceA.getInstanceNumber() == 1) {
            assertEquals(2, remoteDeviceB.getInstanceNumber());
        } else {
            assertEquals(1, remoteDeviceB.getInstanceNumber());
        }
        assertContainsExactlyInAnyOrder(discoverer.getRemoteDevices(), remoteDeviceA, remoteDeviceB);
        assertContainsExactlyInAnyOrder(discoverer.getLatestRemoteDevices(), remoteDeviceA, remoteDeviceB);
    }

    private void assertContainsExactlyInAnyOrder(Collection<? extends RemoteDevice> devices, RemoteDevice... expected) {
        assertEquals(expected.length, devices.size());
        for (RemoteDevice device : expected) {
            assertTrue(devices.contains(device));
        }
    }

}
