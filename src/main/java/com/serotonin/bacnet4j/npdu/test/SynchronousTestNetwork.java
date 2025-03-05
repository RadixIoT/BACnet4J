package com.serotonin.bacnet4j.npdu.test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

/**
 * @author Terry Packer
 */
public class SynchronousTestNetwork extends AbstractTestNetwork<SynchronousTestNetwork> {

    private long bytesOut;
    private long bytesIn;

    public SynchronousTestNetwork(TestNetworkMap map, int address, int sendDelay) {
        super(map, address, sendDelay);
    }


    @Override
    public long getBytesOut() {
        return bytesOut;
    }

    @Override
    public long getBytesIn() {
        return bytesIn;
    }

    @Override
    public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast, boolean expectsReply) throws BACnetException {
        final TestNetwork.SendData d = new TestNetwork.SendData();
        d.recipient = recipient;
        d.data = npdu.popAll();
        bytesOut += d.data.length;

        ThreadUtils.sleep(sendDelay);

        if (d.recipient.equals(getLocalBroadcastAddress()) || d.recipient.equals(Address.GLOBAL)) {
            // A broadcast. Send to everyone.
            for (final SynchronousTestNetwork network : networkMap)
                receive(network, d.data);
        } else {
            // A directed message. Find the network to pass it to.
            final SynchronousTestNetwork network = networkMap.get(d.recipient);
            if (network != null)
                receive(network, d.data);
        }
    }

    @Override
    protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception {
        return parseNpduData(queue, linkService);
    }
}
