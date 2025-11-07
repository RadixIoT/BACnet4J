package com.serotonin.bacnet4j.transport;

import static org.junit.Assert.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class ServiceFutureImplTest {

    @Test(timeout = 10_000)
    public void allWaitersAreReleased() throws Exception {
        var sut = new ServiceFutureImpl();

        var futures = new CompletableFuture[10];
        for(int i=0;i<10;i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    sut.get();
                } catch (BACnetException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        // Give some time for the other threads to start waiting on sut.get()
        Thread.sleep(10);

        sut.success(null);

        CompletableFuture.allOf(futures).get(10, TimeUnit.SECONDS);
    }

    @Test(timeout = 10_000)
    public void getGuardsAgainstSpuriousWakeup() throws Exception {
        var sut = new ServiceFutureImpl();

        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
            try {
                sut.get();
            } catch (BACnetException e) {
                throw new RuntimeException(e);
            }
        });

        // Give some time for the other thread to start waiting on sut.get()
        Thread.sleep(10);

        ThreadUtils.notifySync(sut);

        assertThrows(TimeoutException.class, () -> f1.get(2, TimeUnit.SECONDS));
    }
}
