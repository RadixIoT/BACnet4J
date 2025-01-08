package com.serotonin.bacnet4j.npdu.ip;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

public class IpNetworkBuilderTest {
    @Test
    public void withSubnet16() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.0.0", 16);
        assertEquals("192.168.255.255", builder.getBroadcastAddress());
        assertEquals("255.255.0.0", builder.getSubnetMask());
    }

    @Test
    public void withBroadcast16() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.255.255", 16);
        assertEquals("192.168.255.255", builder.getBroadcastAddress());
        assertEquals("255.255.0.0", builder.getSubnetMask());
    }

    @Test
    public void withSubnet24() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.2.0", 24);
        assertEquals("192.168.2.255", builder.getBroadcastAddress());
        assertEquals("255.255.255.0", builder.getSubnetMask());
    }

    @Test
    public void withBroadcast24() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.4.255", 24);
        assertEquals("192.168.4.255", builder.getBroadcastAddress());
        assertEquals("255.255.255.0", builder.getSubnetMask());
    }

    @Test
    public void withSubnet19() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.192.0", 19);
        assertEquals("192.168.223.255", builder.getBroadcastAddress());
        assertEquals("255.255.224.0", builder.getSubnetMask());
    }

    @Test
    public void withBroadcast19() {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.223.255", 19);
        assertEquals("192.168.223.255", builder.getBroadcastAddress());
        assertEquals("255.255.224.0", builder.getSubnetMask());
    }

    @Test
    public void withLocalIPAddress(){
        try {
            Random rand = new Random();
            Map<Integer, List<String>> interfaceDetails = IpNetworkUtils.getLocalInterfaceAddresses();
            List<Integer> keys = new ArrayList<>(interfaceDetails.keySet());
            final int interfaceIndex = keys.get(rand.nextInt(keys.size()));
            String IPAddress = interfaceDetails.get(interfaceIndex).get(1); 
            final IpNetworkBuilder builder = new IpNetworkBuilder().withLocalBindAddress(IPAddress);
            assertEquals(IPAddress,builder.getLocalBindAddress());
            assertEquals(interfaceDetails.get(interfaceIndex).get(2), builder.getBroadcastAddress());
            assertEquals(interfaceDetails.get(interfaceIndex).get(3), builder.getSubnetMask());
            System.out.println(IPAddress);
            
        }catch (final Exception e) {
            // Should never happen, so just wrap in a RuntimeException
            throw new RuntimeException(e);
        }
    }
    @Test
    public void withBroadcast(){
        try {
            Random rand = new Random();
            Map<Integer, List<String>> interfaceDetails = IpNetworkUtils.getLocalInterfaceAddresses();
            List<Integer> keys = new ArrayList<>(interfaceDetails.keySet());
            final int interfaceIndex = keys.get(rand.nextInt(keys.size()));
            String IPAddress = interfaceDetails.get(interfaceIndex).get(2); 
            final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast(IPAddress, 24);
            assertEquals(IPAddress,builder.getBroadcastAddress());
            assertEquals(interfaceDetails.get(interfaceIndex).get(1), builder.getLocalBindAddress());
            assertEquals(interfaceDetails.get(interfaceIndex).get(3), builder.getSubnetMask());
            System.out.println(IPAddress);
            
        }catch (final Exception e) {
            // Should never happen, so just wrap in a RuntimeException
            throw new RuntimeException(e);
        }
    
    }

    
    @Test
    public void withLocalBindAddress_IncorrectIP(){
        String IPAddress = "1.1.1.1";
        Exception exception = assertThrows(RuntimeException.class,() -> {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withLocalBindAddress(IPAddress);
    });
        String actualMsg = exception.getMessage();
        System.out.println(actualMsg);
        assertTrue((actualMsg.contains("IllegalArgument")));
    }

    @Test
    public void verifyReuseAddress(){
        final IpNetworkBuilder builder = new IpNetworkBuilder();
        if (System.getProperty("os.name").contains("Windows"))

            assertTrue(!builder.isReuseAddress());//reuseAddress = false;
        else
            assertTrue(builder.isReuseAddress()); 
        System.out.println(builder.isReuseAddress());
    }

    @Test
    public void withIterfaceName(){
        Random rand = new Random();
        Map<Integer, List<String>> interfaceDetails = IpNetworkUtils.getLocalInterfaceAddresses();
        List<Integer> keys = new ArrayList<>(interfaceDetails.keySet());
        System.out.println(keys);
        final int interfaceIndex = keys.get(rand.nextInt(keys.size()));
        String ifaceName = interfaceDetails.get(interfaceIndex).get(0); 
        final IpNetworkBuilder builder = new IpNetworkBuilder().withInterfaceName(ifaceName);
        assertEquals(interfaceDetails.get(interfaceIndex).get(1), builder.getLocalBindAddress());
        assertEquals(interfaceDetails.get(interfaceIndex).get(2),builder.getBroadcastAddress());
        assertEquals(interfaceDetails.get(interfaceIndex).get(3), builder.getSubnetMask());
        System.out.println("Local Bind Address : " + interfaceDetails.get(interfaceIndex).get(1) +
            " BroadcastAddress : " + interfaceDetails.get(interfaceIndex).get(2) + " SubnetMask: "+ interfaceDetails.get(interfaceIndex).get(3));

    }
}
