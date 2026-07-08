/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2026 Radix IoT LLC. All rights reserved.
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

package com.serotonin.bacnet4j.service.confirmed;

import java.util.Objects;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.NotImplementedException;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.AuthorizationScope;
import com.serotonin.bacnet4j.type.constructed.BaseType;
import com.serotonin.bacnet4j.type.constructed.Choice;
import com.serotonin.bacnet4j.type.constructed.ChoiceOptions;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.primitive.SignedInteger;
import com.serotonin.bacnet4j.type.primitive.Unsigned32;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthRequestRequest extends ConfirmedRequestService {
    public static final byte TYPE_ID = 34;

    private static final ChoiceOptions choiceOptions = new ChoiceOptions();

    static {
        choiceOptions.addContextual(0, TokenRequest.class);
    }

    private final Choice subService;

    public AuthRequestRequest(TokenRequest tokenRequest) {
        subService = new Choice(0, tokenRequest, choiceOptions);
    }

    @Override
    public byte getChoiceId() {
        return TYPE_ID;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, subService);
    }

    public AuthRequestRequest(ByteQueue queue) throws BACnetException {
        subService = new Choice(queue, choiceOptions);
    }

    @Override
    public AcknowledgementService handle(LocalDevice localDevice, Address from) throws BACnetException {
        throw new NotImplementedException();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthRequestRequest that = (AuthRequestRequest) o;
        return Objects.equals(subService, that.subService);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(subService);
    }

    @Override
    public String toString() {
        return "AuthRequestRequest [subService=" + subService + "]";
    }

    public static class TokenRequest extends BaseType {
        private final Unsigned32 client;
        private final SequenceOf<SignedInteger> audience;
        private final AuthorizationScope scope;

        public TokenRequest(Unsigned32 client, SequenceOf<SignedInteger> audience, AuthorizationScope scope) {
            this.client = client;
            this.audience = audience;
            this.scope = scope;
        }

        @Override
        public void write(ByteQueue queue) {
            write(queue, client, 0);
            write(queue, audience, 1);
            write(queue, scope, 2);
        }

        public TokenRequest(ByteQueue queue) throws BACnetException {
            client = read(queue, Unsigned32.class, 0);
            audience = readSequenceOf(queue, SignedInteger.class, 1);
            scope = read(queue, AuthorizationScope.class, 2);
        }

        public Unsigned32 getClient() {
            return client;
        }

        public SequenceOf<SignedInteger> getAudience() {
            return audience;
        }

        public AuthorizationScope getScope() {
            return scope;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass())
                return false;
            TokenRequest that = (TokenRequest) o;
            return Objects.equals(client, that.client) && Objects.equals(audience,
                    that.audience) && Objects.equals(scope, that.scope);
        }

        @Override
        public int hashCode() {
            return Objects.hash(client, audience, scope);
        }

        @Override
        public String toString() {
            return "TokenRequest ["
                    + "client=" + client
                    + ", audience=" + audience
                    + ", scope=" + scope
                    + ']';
        }
    }
}
