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
        for(int i=0;i<10;i++) {
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
