package com.serotonin.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class FixedDelay extends Repeating {
    private final long delay;

    public FixedDelay(TaskExecutingScheduledExecutorService scheduledExecutorService, ExecutorService executorService, final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {
        super(scheduledExecutorService, executorService, command, initialDelay, unit);
        this.delay = delay;
    }

    @Override
    void updateNextRuntime() {
        nextRuntime = scheduledExecutorService.getClock().millis() + unit.toMillis(delay);
    }
}
