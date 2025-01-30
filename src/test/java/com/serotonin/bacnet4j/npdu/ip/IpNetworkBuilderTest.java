package com.serotonin.bacnet4j.npdu.ip;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;


import static org.junit.Assert.*;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
public class IpNetworkBuilderTest {

    IpNetworkUtils.InterfaceInfo interfaceInfo; 
    List<IpNetworkUtils.InterfaceInfo> interfaceDetails; 

    @Test
    public void withSubnet16() {
        try(MockedStatic<IpNetworkUtils> mockedUtil = mockStatic(IpNetworkUtils.class, CALLS_REAL_METHODS)){
                mockedUtil.when(()->IpNetworkUtils.getIPAddressString(anyString())).thenReturn(IpNetwork.DEFAULT_BIND_IP);
                final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.0.0", 16);
       
                assertEquals("192.168.255.255", builder.getBroadcastAddress());
                assertEquals("255.255.0.0",builder.getSubnetMask());
            }
    }

    @Test
    public void withBroadcast16() {
        try(MockedStatic<IpNetworkUtils> mockedUtil = mockStatic(IpNetworkUtils.class, CALLS_REAL_METHODS)){
            mockedUtil.when(()->IpNetworkUtils.getIPAddressString(anyString())).thenReturn(IpNetwork.DEFAULT_BIND_IP);
            final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.255.255", 16);
            assertEquals("192.168.255.255", builder.getBroadcastAddress());
            assertEquals("255.255.0.0", builder.getSubnetMask());
        }
    
    }

    @Test
    public void withSubnet24() {
        try(MockedStatic<IpNetworkUtils> mockedUtil = mockStatic(IpNetworkUtils.class, CALLS_REAL_METHODS)){
            mockedUtil.when(()->IpNetworkUtils.getIPAddressString(anyString())).thenReturn(IpNetwork.DEFAULT_BIND_IP);
            final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.2.0", 24);
            assertEquals("192.168.2.255", builder.getBroadcastAddress());
            assertEquals("255.255.255.0", builder.getSubnetMask());
        }
    }

    @Test
    public void withBroadcast24() {
        try(MockedStatic<IpNetworkUtils> mockedUtil = mockStatic(IpNetworkUtils.class, CALLS_REAL_METHODS)){
            mockedUtil.when(()->IpNetworkUtils.getIPAddressString(anyString())).thenReturn(IpNetwork.DEFAULT_BIND_IP);
            final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.4.255", 24);
            assertEquals("192.168.4.255", builder.getBroadcastAddress());
            assertEquals("255.255.255.0", builder.getSubnetMask());
        }
    }

    @Test
    public void withSubnet19() {
        try(MockedStatic<IpNetworkUtils> mockedUtil = mockStatic(IpNetworkUtils.class, CALLS_REAL_METHODS)){
            mockedUtil.when(()->IpNetworkUtils.getIPAddressString(anyString())).thenReturn(IpNetwork.DEFAULT_BIND_IP);
            final IpNetworkBuilder builder = new IpNetworkBuilder().withSubnet("192.168.192.0", 19);
            assertEquals("192.168.223.255", builder.getBroadcastAddress());
            assertEquals("255.255.224.0", builder.getSubnetMask());
        }
    }

    @Test
    public void withBroadcast19() {
        try(MockedStatic<IpNetworkUtils> mockedUtil = mockStatic(IpNetworkUtils.class, CALLS_REAL_METHODS)){
            mockedUtil.when(()->IpNetworkUtils.getIPAddressString(anyString())).thenReturn(IpNetwork.DEFAULT_BIND_IP);
            final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast("192.168.223.255", 19);
            assertEquals("192.168.223.255", builder.getBroadcastAddress());
            assertEquals("255.255.224.0", builder.getSubnetMask());
        }
    }

    @Before 
    public void initialize(){
        interfaceDetails = IpNetworkUtils.getLocalInterfaceAddresses();
        interfaceInfo = interfaceDetails.get(0);  
    }

    @Test
    public void withLocalIPAddress() throws RuntimeException{
        
        String IPAddress = interfaceInfo.localIPAddress(); 
        final IpNetworkBuilder builder = new IpNetworkBuilder().withLocalBindAddress(IPAddress);
        assertEquals(IPAddress,builder.getLocalBindAddress());
        assertEquals(interfaceInfo.broadcastAddress(), builder.getBroadcastAddress());
        assertEquals(interfaceInfo.subnetMask(), builder.getSubnetMask());   
    }

    @Test
    public void withBroadcast() throws RuntimeException{
                
        String IPAddress = interfaceInfo.broadcastAddress();
        final IpNetworkBuilder builder = new IpNetworkBuilder().withBroadcast(IPAddress, interfaceInfo.networkPrefixLength());
        assertEquals(IPAddress,builder.getBroadcastAddress());
        assertEquals(interfaceInfo.localIPAddress(), builder.getLocalBindAddress());
        assertEquals(interfaceInfo.subnetMask(), builder.getSubnetMask());
    }

    
    @Test
    public void withLocalBindAddress_IncorrectIP() throws RuntimeException{
        String IPAddress = "1.1.1.1";
        Exception exception = assertThrows(RuntimeException.class,() -> {
        final IpNetworkBuilder builder = new IpNetworkBuilder().withLocalBindAddress(IPAddress);});
        String actualMsg = exception.getMessage();
        assertTrue((actualMsg.contains("IllegalArgument")));
    }

    @Test
    public void verifyReuseAddress(){
        final IpNetworkBuilder builder = new IpNetworkBuilder();
        if (System.getProperty("os.name").contains("Windows"))
            assertTrue(!builder.isReuseAddress());//reuseAddress = false;
        else
            assertTrue(builder.isReuseAddress()); 
        
    }

    @Test
    public void withIterfaceName() throws RuntimeException{
        String ifaceName = interfaceInfo.interfaceName(); 
        final IpNetworkBuilder builder = new IpNetworkBuilder().withInterfaceName(ifaceName);
        assertEquals(interfaceInfo.localIPAddress(), builder.getLocalBindAddress());
        assertEquals(interfaceInfo.broadcastAddress(),builder.getBroadcastAddress());
        assertEquals(interfaceInfo.subnetMask(), builder.getSubnetMask());

    }
}
