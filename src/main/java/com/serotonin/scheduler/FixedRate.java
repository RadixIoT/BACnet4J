package com.serotonin.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class FixedRate extends Repeating {
    private final long period;

    public FixedRate(TaskExecutingScheduledExecutorService scheduledExecutorService, ExecutorService executorService, final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {
        super(scheduledExecutorService, executorService, command, initialDelay, unit);
        this.period = period;
    }

    @Override
    void updateNextRuntime() {
        nextRuntime += unit.toMillis(period);
    }
}
