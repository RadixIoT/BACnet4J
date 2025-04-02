/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.transport;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.ResponseConsumer;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkIdentifier;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.primitive.OctetString;

import java.util.Map;

/**
 * Provides segmentation support for all data link types.
 *
 * @author Matthew
 */
public interface Transport {
    public static final int DEFAULT_TIMEOUT = 6000;
    public static final int DEFAULT_SEG_TIMEOUT = 5000;
    public static final int DEFAULT_SEG_WINDOW = 5;
    public static final int DEFAULT_RETRIES = 2;

    NetworkIdentifier getNetworkIdentifier();

    Network getNetwork();

    LocalDevice getLocalDevice();

    void setLocalDevice(LocalDevice localDevice);

    public void setTimeout(int timeout);

    public int getTimeout();

    public void setSegTimeout(int segTimeout);

    public int getSegTimeout();

    public void setRetries(int retries);

    public int getRetries();

    public void setSegWindow(int segWindow);

    public int getSegWindow();

    void initialize() throws Exception;

    void terminate();

    long getBytesOut();

    long getBytesIn();

    Address getLocalBroadcastAddress();

    void addNetworkRouter(int networkNumber, OctetString mac);

    Map<Integer, OctetString> getNetworkRouters();

    void send(Address address, UnconfirmedRequestService service);

    ServiceFuture send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            ConfirmedRequestService service);

    void send(Address address, int maxAPDULengthAccepted, Segmentation segmentationSupported,
            ConfirmedRequestService service, ResponseConsumer consumer);

    void incoming(NPDU npdu);
}
