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

package com.serotonin.bacnet4j.transport;

import static com.serotonin.bacnet4j.TestUtils.await;
import static org.junit.Assert.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class ServiceFutureImplTest {
    @Test(timeout = 10_000)
    public void allWaitersAreReleased() throws Exception {
        var sut = new ServiceFutureImpl();

        var futures = new CompletableFuture[10];
        var waiters = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    waiters.incrementAndGet();
                    sut.get();
                } catch (BACnetException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Wait for the other threads to start waiting on sut.get()
        await(() -> waiters.get() == 10);

        sut.success(null);

        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
    }

    @Test(timeout = 10_000)
    public void getGuardsAgainstSpuriousWakeup() throws Exception {
        var sut = new ServiceFutureImpl();

        var waiters = new AtomicInteger(0);
        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
            try {
                waiters.incrementAndGet();
                sut.get();
            } catch (BACnetException e) {
                throw new RuntimeException(e);
            }
        });

        // Wait for the other thread to start waiting on sut.get()
        await(() -> waiters.get() == 1);

        ThreadUtils.notifySync(sut);

        assertThrows(TimeoutException.class, () -> f1.get(2, TimeUnit.SECONDS));
    }
}
