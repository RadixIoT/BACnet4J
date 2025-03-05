package com.serotonin.bacnet4j;

import com.serotonin.bacnet4j.npdu.test.AbstractTestNetwork;
import com.serotonin.bacnet4j.npdu.test.SynchronousTestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.AbstractTransport;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.SynchronousTransport;
import com.serotonin.bacnet4j.util.DiscoveryUtils;
import lohbihler.warp.TestingWarpScheduledExecutorService;
import lohbihler.warp.WarpClock;
import lohbihler.warp.WarpScheduledExecutorService;
import org.junit.After;
import org.junit.Before;

/**
 * Common base class for tests that use real local devices and a warp clock.
 *
 * @author Matthew
 */
abstract public class AbstractTest {
    protected static final int TIMEOUT = 500;

    //Do we use a synchronous test implementation or the actual Network and Transport implementations
    protected final boolean synchronousTesting;

    private TestNetworkMap map;
    protected WarpClock clock;
    protected WarpScheduledExecutorService executor;

    protected LocalDevice d1;
    protected LocalDevice d2;
    protected LocalDevice d3;
    protected LocalDevice d4;
    protected RemoteDevice rd1;
    protected RemoteDevice rd2;
    protected RemoteDevice rd3;

    public AbstractTest() {
        this(true);
    }

    public AbstractTest(boolean synchronousTesting) {
        this.synchronousTesting = synchronousTesting;
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
        this.clock = new WarpClock();
        this.executor = createExecutorService();
        this.d1 = new LocalDevice(1,
                createTransport(createNetwork(map, 1, 0, TIMEOUT))).withClock(clock)
                .withScheduledExecutor(executor);
        this.d2 = new LocalDevice(2,
                createTransport(createNetwork(map, 2, 0, TIMEOUT))).withClock(clock)
                .withScheduledExecutor(executor);
        this.d3 = new LocalDevice(3, createTransport(createNetwork(map, 3, 0))).withClock(clock)
                .withScheduledExecutor(executor);
        this.d4 = new LocalDevice(4, createTransport(createNetwork(map, 4, 0))).withClock(clock)
                .withScheduledExecutor(executor);
    }

    protected WarpScheduledExecutorService createExecutorService() {
        if(synchronousTesting) {
            return new TestingWarpScheduledExecutorService(clock);
        }else {
            return new WarpScheduledExecutorService(clock);
        }
    }
    protected AbstractTransport createTransport(AbstractTestNetwork network) {
        if(synchronousTesting) {
            return new SynchronousTransport(network);
        }else {
            return new DefaultTransport(network);
        }
    }

    protected AbstractTestNetwork createNetwork(TestNetworkMap map, int address, int sendDelay, int timeout) {
        return this.createNetwork(map, address, sendDelay).withTimeout(timeout);
    }

    protected AbstractTestNetwork createNetwork(TestNetworkMap map, int address, int sendDelay) {
        if(synchronousTesting) {
            return new SynchronousTestNetwork(map, address, sendDelay);
        }else {
            return new TestNetwork(map, address, sendDelay);
        }

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
