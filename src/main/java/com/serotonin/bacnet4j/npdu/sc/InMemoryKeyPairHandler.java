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

package com.serotonin.bacnet4j.npdu.sc;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Objects;

/**
 * A {@link SCKeyPairHandler} that holds the key pairs in memory only. Suitable for tests and as a reference
 * for the promotion logic; a shipping product must provide an implementation that persists both pairs
 * (encrypted at rest) so they survive a device restart.
 */
public class InMemoryKeyPairHandler implements SCKeyPairHandler {
    private KeyPair activeKeyPair;
    private KeyPair pendingKeyPair;

    public InMemoryKeyPairHandler(KeyPair activeKeyPair) {
        this(activeKeyPair, null);
    }

    public InMemoryKeyPairHandler(KeyPair activeKeyPair, KeyPair pendingKeyPair) {
        this.activeKeyPair = Objects.requireNonNull(activeKeyPair, "activeKeyPair is required");
        this.pendingKeyPair = pendingKeyPair;
    }

    @Override
    public KeyPair getActiveKeyPair() {
        return activeKeyPair;
    }

    @Override
    public KeyPair getPendingKeyPair() {
        return pendingKeyPair;
    }

    @Override
    public void setPendingKeyPair(KeyPair keyPair) {
        pendingKeyPair = keyPair;
    }

    @Override
    public void certificateActivated(X509Certificate certificate) {
        KeyPair pending = pendingKeyPair;
        if (pending != null && SCNetworkUtils.publicKeyMatches(certificate, pending)) {
            activeKeyPair = pending;
            pendingKeyPair = null;
        }
    }
}
