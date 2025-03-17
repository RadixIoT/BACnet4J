package com.serotonin.bacnet4j;

import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.util.DiscoveryUtils;
import com.serotonin.warp.WarpClock;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.ScheduledExecutorService;

import static org.junit.Assert.fail;

/**
 * Common base class for tests that use real local devices and a warp clock.
 *
 * @author Matthew
 */
abstract public class AbstractTest implements SynchronousLocalDeviceInitializer {
    protected static final int TIMEOUT = 500;

    private TestNetworkMap map;
    protected WarpClock clock;

    protected LocalDevice d1;
    protected ScheduledExecutorService d1Executor;

    protected LocalDevice d2;
    protected ScheduledExecutorService d2Executor;

    protected LocalDevice d3;
    protected ScheduledExecutorService d3Executor;

    protected LocalDevice d4;
    protected ScheduledExecutorService d4Executor;

    protected RemoteDevice rd1;
    protected RemoteDevice rd2;
    protected RemoteDevice rd3;

    public AbstractTest() {

    }

    @Before
    public void abstractBefore() throws Exception {
        setup();
        beforeInit();
        d1.initialize();
        d2.initialize();
        d3.initialize();
        d4.initialize();

        // Get d1 as a remote object.
        rd1 = d2.getRemoteDevice(1).get();
        rd2 = d1.getRemoteDevice(2).get();
        rd3 = d1.getRemoteDevice(3).get();

        DiscoveryUtils.getExtendedDeviceInformation(d1, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d1, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d1, rd3);
        
        DiscoveryUtils.getExtendedDeviceInformation(d2, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d2, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d2, rd3);
        
        DiscoveryUtils.getExtendedDeviceInformation(d3, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d3, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d3, rd3);
        
        DiscoveryUtils.getExtendedDeviceInformation(d4, rd1);
        DiscoveryUtils.getExtendedDeviceInformation(d4, rd2);
        DiscoveryUtils.getExtendedDeviceInformation(d4, rd3);
        
        afterInit();
    }

    /**
     * Setup the components for the tests
     */
    public void setup() {
        this.map = new TestNetworkMap();
        this.clock = getClock();
        this.d1Executor = createExecutorService(clock);
        this.d1 = new LocalDevice(1,
                createTransport(createTestNetwork(map, 1, 0, TIMEOUT)), clock, d1Executor);

        this.d2Executor = createExecutorService(clock);
        this.d2 = new LocalDevice(2,
                createTransport(createTestNetwork(map, 2, 0, TIMEOUT)), clock, d2Executor);

        this.d3Executor = createExecutorService(clock);
        this.d3 = new LocalDevice(3, createTransport(createTestNetwork(map, 3, 0)), clock, d3Executor);

        this.d4Executor = createExecutorService(clock);
        this.d4 = new LocalDevice(4, createTransport(createTestNetwork(map, 4, 0)), clock, d4Executor);
    }

    /**
     * Helper for assertions, find a property in a sequence by the identifier, if it doesn't exist fail via assertion
     * @param propertyIdentifier
     * @param covProperties
     * @return
     */
    protected PropertyValue findProperty(PropertyIdentifier propertyIdentifier, SequenceOf<PropertyValue> covProperties) {
        for(PropertyValue propertyValue : covProperties) {
            if(propertyIdentifier.equals(propertyValue.getPropertyIdentifier())) {
                return propertyValue;
            }
        }
        fail("Didn't find property " + propertyIdentifier.toString());
        return null;
    }

    /**
     * Before initializing devices and discovery for each test
     * @throws Exception
     */
    public void beforeInit() throws Exception {
        // Override as required
    }

    /**
     * After initializing devices and discovery for each test
     * @throws Exception
     */
    public void afterInit() throws Exception {
        // Override as required
    }

    @After
    public void abstractAfter() {
        // Shut down
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
    }
}
