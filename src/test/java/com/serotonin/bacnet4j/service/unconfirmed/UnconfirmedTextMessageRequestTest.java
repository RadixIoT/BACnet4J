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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import com.serotonin.bacnet4j.AbstractTest;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.enumerated.MessagePriority;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class UnconfirmedTextMessageRequestTest extends AbstractTest {
    @Test
    public void happy() throws Exception {
        final Object monitor = new Object();

        // Create the listener in device 2
        final AtomicReference<ObjectIdentifier> receivedObjectIdentifier = new AtomicReference<>(null);
        final AtomicReference<Choice> receivedMessageClass = new AtomicReference<>(null);
        final AtomicReference<MessagePriority> receivedMessagePriority = new AtomicReference<>(null);
        final AtomicReference<CharacterString> receivedMessage = new AtomicReference<>(null);
        d2.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void textMessageReceived(final ObjectIdentifier textMessageSourceDevice, final Choice messageClass,
                    final MessagePriority messagePriority, final CharacterString message) {
                receivedObjectIdentifier.set(textMessageSourceDevice);
                receivedMessageClass.set(messageClass);
                receivedMessagePriority.set(messagePriority);
                receivedMessage.set(message);
                synchronized (monitor) {
                    monitor.notify();
                }
            }
        });

        // First request constructor.
        synchronized (monitor) {
            d1.send(rd2, new UnconfirmedTextMessageRequest(new ObjectIdentifier(ObjectType.accessCredential, 0),
                    MessagePriority.normal, new CharacterString("The message")));
            monitor.wait(1000);
        }

        assertEquals(new ObjectIdentifier(ObjectType.accessCredential, 0), receivedObjectIdentifier.get());
        assertEquals(null, receivedMessageClass.get());
        assertEquals(MessagePriority.normal, receivedMessagePriority.get());
        assertEquals(new CharacterString("The message"), receivedMessage.get());

        // Second request constructor.
        synchronized (monitor) {
            d1.send(rd2, new UnconfirmedTextMessageRequest(new ObjectIdentifier(ObjectType.accessCredential, 0),
                    new UnsignedInteger(12), MessagePriority.normal, new CharacterString("The message")));
            monitor.wait(1000);
        }

        assertEquals(new ObjectIdentifier(ObjectType.accessCredential, 0), receivedObjectIdentifier.get());
        assertEquals(new UnsignedInteger(12), receivedMessageClass.get().getDatum());
        assertEquals(MessagePriority.normal, receivedMessagePriority.get());
        assertEquals(new CharacterString("The message"), receivedMessage.get());

        // Third request constructor.
        synchronized (monitor) {
            d1.send(rd2,
                    new UnconfirmedTextMessageRequest(new ObjectIdentifier(ObjectType.accessCredential, 0),
                            new CharacterString("Some message class"), MessagePriority.normal,
                            new CharacterString("The message")));
            monitor.wait(1000);
        }

        assertEquals(new ObjectIdentifier(ObjectType.accessCredential, 0), receivedObjectIdentifier.get());
        assertEquals(new CharacterString("Some message class"), receivedMessageClass.get().getDatum());
        assertEquals(MessagePriority.normal, receivedMessagePriority.get());
        assertEquals(new CharacterString("The message"), receivedMessage.get());
    }
}
