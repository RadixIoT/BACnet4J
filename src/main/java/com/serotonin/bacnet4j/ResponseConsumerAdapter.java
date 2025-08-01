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

package com.serotonin.bacnet4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.apdu.AckAPDU;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;

abstract public class ResponseConsumerAdapter<T extends AcknowledgementService> implements ResponseConsumer {
    static final Logger LOG = LoggerFactory.getLogger(ResponseConsumerAdapter.class);

    @SuppressWarnings("unchecked")
    @Override
    public final void success(AcknowledgementService ack) {
        ack((T) ack);
    }

    abstract public void ack(T ack);

    @Override
    public void fail(AckAPDU ack) {
        LOG.error("Request failed {}", ack);
    }

    @Override
    public void ex(BACnetException e) {
        LOG.error("Request failed", e);
    }
}
