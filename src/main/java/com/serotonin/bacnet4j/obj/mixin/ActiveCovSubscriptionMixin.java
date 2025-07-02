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

package com.serotonin.bacnet4j.obj.mixin;

import java.util.List;
import java.util.Map;

import com.serotonin.bacnet4j.obj.AbstractMixin;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.type.constructed.CovSubscription;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.Recipient;
import com.serotonin.bacnet4j.type.constructed.RecipientProcess;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * Manages the active COV subscription property in the device by writing it fresh each time it is requested.
 *
 * @author Matthew
 */
public class ActiveCovSubscriptionMixin extends AbstractMixin {
    public ActiveCovSubscriptionMixin(final BACnetObject bo) {
        super(bo);

        writePropertyInternal(PropertyIdentifier.activeCovSubscriptions, new SequenceOf<CovSubscription>());
    }

    @Override
    protected void beforeReadProperty(final PropertyIdentifier pid) {
        if (pid.equals(PropertyIdentifier.activeCovSubscriptions)) {
            // Update the time remaining amounts by rewriting the subscriptions list for each request.
            final long now = getLocalDevice().getClock().millis();

            final Map<ObjectIdentifier, List<CovContext>> ctxs = getLocalDevice().getCovContexts();
            final SequenceOf<CovSubscription> subscriptions = new SequenceOf<>();
            for (final Map.Entry<ObjectIdentifier, List<CovContext>> e : ctxs.entrySet()) {
                synchronized (e.getValue()) {
                    for (final CovContext ctx : e.getValue()) {
                        if (!ctx.hasExpired(now)) {
                            final RecipientProcess rp = new RecipientProcess(new Recipient(ctx.getAddress()),
                                    ctx.getSubscriberProcessIdentifier());
                            final ObjectPropertyReference opr = new ObjectPropertyReference(e.getKey(),
                                    ctx.getExposedMonitoredProperty(), null);
                            final CovSubscription cs = new CovSubscription(rp, opr,
                                    Boolean.valueOf(ctx.isIssueConfirmedNotifications()),
                                    new UnsignedInteger(ctx.getSecondsRemaining(now)), ctx.getCovIncrement());
                            subscriptions.add(cs);
                        }
                    }
                }
            }

            writePropertyInternal(PropertyIdentifier.activeCovSubscriptions, subscriptions);
        }
    }
}
