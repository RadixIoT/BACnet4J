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

package com.serotonin.bacnet4j.type.constructed;

import java.util.Objects;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.enumerated.AuthorizationPosture;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class AuthorizationStatus extends BaseType {
    private final AuthorizationPosture posture;
    private final ErrorClassAndCode error;
    private final ObjectPropertyReference errorSource;
    private final CharacterString errorDetails;
    private final SequenceOf<AuthenticationEvent> authenticationSuccess;
    private final SequenceOf<AuthenticationEvent> authenticationFailure;
    private final SequenceOf<AuthorizationEvent> authorizationSuccess;
    private final SequenceOf<AuthorizationEvent> authorizationFailure;

    public AuthorizationStatus(
            AuthorizationPosture posture,
            ErrorClassAndCode error,
            ObjectPropertyReference errorSource,
            CharacterString errorDetails,
            SequenceOf<AuthenticationEvent> authenticationSuccess,
            SequenceOf<AuthenticationEvent> authenticationFailure,
            SequenceOf<AuthorizationEvent> authorizationSuccess,
            SequenceOf<AuthorizationEvent> authorizationFailure) {
        this.posture = posture;
        this.error = error;
        this.errorSource = errorSource;
        this.errorDetails = errorDetails;
        this.authenticationSuccess = authenticationSuccess;
        this.authenticationFailure = authenticationFailure;
        this.authorizationSuccess = authorizationSuccess;
        this.authorizationFailure = authorizationFailure;
    }

    @Override
    public void write(final ByteQueue queue) {
        write(queue, posture, 0);
        writeOptional(queue, error, 1);
        writeOptional(queue, errorSource, 2);
        writeOptional(queue, errorDetails, 3);
        writeOptional(queue, authenticationSuccess, 4);
        writeOptional(queue, authenticationFailure, 5);
        writeOptional(queue, authorizationSuccess, 6);
        writeOptional(queue, authorizationFailure, 7);
    }

    @Override
    public String toString() {
        return "AuthorizationStatus [" +
                "posture=" + posture +
                ", error=" + error +
                ", errorSource=" + errorSource +
                ", errorDetails=" + errorDetails +
                ", authenticationSuccess=" + authenticationSuccess +
                ", authenticationFailure=" + authenticationFailure +
                ", authorizationSuccess=" + authorizationSuccess +
                ", authorizationFailure=" + authorizationFailure +
                ']';
    }

    public AuthorizationPosture getPosture() {
        return posture;
    }

    public ErrorClassAndCode getError() {
        return error;
    }

    public ObjectPropertyReference getErrorSource() {
        return errorSource;
    }

    public CharacterString getErrorDetails() {
        return errorDetails;
    }

    public SequenceOf<AuthenticationEvent> getAuthenticationSuccess() {
        return authenticationSuccess;
    }

    public SequenceOf<AuthenticationEvent> getAuthenticationFailure() {
        return authenticationFailure;
    }

    public SequenceOf<AuthorizationEvent> getAuthorizationSuccess() {
        return authorizationSuccess;
    }

    public SequenceOf<AuthorizationEvent> getAuthorizationFailure() {
        return authorizationFailure;
    }

    public AuthorizationStatus(final ByteQueue queue) throws BACnetException {
        posture = read(queue, AuthorizationPosture.class, 0);
        error = readOptional(queue, ErrorClassAndCode.class, 1);
        errorSource = readOptional(queue, ObjectPropertyReference.class, 2);
        errorDetails = readOptional(queue, CharacterString.class, 3);
        authenticationSuccess = readOptionalSequenceOf(queue, AuthenticationEvent.class, 4);
        authenticationFailure = readOptionalSequenceOf(queue, AuthenticationEvent.class, 5);
        authorizationSuccess = readOptionalSequenceOf(queue, AuthorizationEvent.class, 6);
        authorizationFailure = readOptionalSequenceOf(queue, AuthorizationEvent.class, 7);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        AuthorizationStatus that = (AuthorizationStatus) o;
        return Objects.equals(posture, that.posture) && Objects.equals(error,
                that.error) && Objects.equals(errorSource, that.errorSource) && Objects.equals(
                errorDetails, that.errorDetails) && Objects.equals(authenticationSuccess,
                that.authenticationSuccess) && Objects.equals(authenticationFailure,
                that.authenticationFailure) && Objects.equals(authorizationSuccess,
                that.authorizationSuccess) && Objects.equals(authorizationFailure, that.authorizationFailure);
    }

    @Override
    public int hashCode() {
        return Objects.hash(posture, error, errorSource, errorDetails, authenticationSuccess, authenticationFailure,
                authorizationSuccess, authorizationFailure);
    }
}
