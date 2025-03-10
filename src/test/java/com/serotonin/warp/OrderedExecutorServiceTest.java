package com.serotonin.warp;

import org.junit.Before;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Terry Packer
 */
public class OrderedExecutorServiceTest {

    private WarpClock clock;
    private Instant start;
    private ObservableScheduledExecutorService scheduler;

    @Before
    public void before() {
        ZoneId zone = ZoneId.systemDefault();
        clock = new WarpClock(zone, LocalDateTime.now(Clock.system(zone)));
        start = clock.instant();
        scheduler = new ObservableScheduledExecutorService(new WarpScheduledExecutorService(clock,
            new OrderedExecutorService(1, Integer.MAX_VALUE, 60L,
                TimeUnit.SECONDS, new LinkedBlockingQueue<>())));
    }

    @Test
    public void testOrderOfImmediateExecution() {
        AtomicInteger counter = new AtomicInteger();
        List<Integer> runOrder = new CopyOnWriteArrayList<>();
        for (int i = 0; i < 1000; i++) {
            int order = counter.getAndIncrement();
            scheduler.execute(() -> {
                runOrder.add(order);
            });
        }

        //Wait for completion
        try {
            assertEquals(
                scheduler.waitForExecutorTasks(50, TimeUnit.MILLISECONDS, 10, true, true, true),
                true);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        assertEquals(1000, runOrder.size());
        for (int i = 0; i < runOrder.size(); i++) {
            assertEquals(i, (int) runOrder.get(i));
        }

    }
}
