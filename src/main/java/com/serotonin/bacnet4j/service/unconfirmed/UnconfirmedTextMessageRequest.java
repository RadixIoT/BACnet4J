/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2025 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.service.unconfirmed;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.NPCI.NetworkPriority;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class UnconfirmedTextMessageRequest extends UnconfirmedRequestService {
    public static final byte TYPE_ID = 5;

    private static ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, UnsignedInteger.class);
        choiceOptions.addContextual(1, CharacterString.class);
    }

    private final ObjectIdentifier textMessageSourceDevice;
    private final Choice messageClass;
    private final MessagePriority messagePriority;
    private final CharacterString message;

    public UnconfirmedTextMessageRequest(final ObjectIdentifier textMessageSourceDevice,
            final UnsignedInteger messageClass, final MessagePriority messagePriority, final CharacterString message) {
        this.textMessageSourceDevice = textMessageSourceDevice;
        this.messageClass = new Choice(0, messageClass, choiceOptions);
        this.messagePriority = messagePriority;
        this.message = message;
    }

    public UnconfirmedTextMessageRequest(final ObjectIdentifier textMessageSourceDevice,
            final CharacterString messageClass, final MessagePriority messagePriority, final CharacterString message) {
        this.textMessageSourceDevice = textMessageSourceDevice;
        this.messageClass = new Choice(1, messageClass, choiceOptions);
        this.messagePriority = messagePriority;
        this.message = message;
    }

    public UnconfirmedTextMessageRequest(final ObjectIdentifier textMessageSourceDevice,
            final MessagePriority messagePriority, final CharacterString message) {
        this.textMessageSourceDevice = textMessageSourceDevice;
        this.messageClass = null;
        this.messagePriority = messagePriority;
        this.message = message;
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public NetworkPriority getNetworkPriority() {
        return messagePriority.getNetworkPriority();
    }

    @Override
    public void handle(final LocalDevice localDevice, final Address from) {
        localDevice.updateRemoteDevice(textMessageSourceDevice.getInstanceNumber(), from);
        localDevice.getEventHandler().fireTextMessage(textMessageSourceDevice, messageClass, messagePriority, message);
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, textMessageSourceDevice, 0);
        writeOptional(queue, messageClass, 1);
        write(queue, messagePriority, 2);
        write(queue, message, 3);
    }

    UnconfirmedTextMessageRequest(final ByteQueue queue) throws BACnetException {
        textMessageSourceDevice = read(queue, ObjectIdentifier.class, 0);
        messageClass = readOptionalChoice(queue, choiceOptions, 1);
        messagePriority = read(queue, MessagePriority.class, 2);
        message = read(queue, CharacterString.class, 3);
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (message == null ? 0 : message.hashCode());
        result = PRIME * result + (messageClass == null ? 0 : messageClass.hashCode());
        result = PRIME * result + (messagePriority == null ? 0 : messagePriority.hashCode());
        result = PRIME * result + (textMessageSourceDevice == null ? 0 : textMessageSourceDevice.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final UnconfirmedTextMessageRequest other = (UnconfirmedTextMessageRequest) obj;
        if (message == null) {
            if (other.message != null)
                return false;
        } else if (!message.equals(other.message))
            return false;
        if (messageClass == null) {
            if (other.messageClass != null)
                return false;
        } else if (!messageClass.equals(other.messageClass))
            return false;
        if (messagePriority == null) {
            if (other.messagePriority != null)
                return false;
        } else if (!messagePriority.equals(other.messagePriority))
            return false;
        if (textMessageSourceDevice == null) {
            if (other.textMessageSourceDevice != null)
                return false;
        } else if (!textMessageSourceDevice.equals(other.textMessageSourceDevice))
            return false;
        return true;
    }
}
