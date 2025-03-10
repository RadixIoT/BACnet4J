package com.serotonin.warp;

import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class FixedDelay extends Repeating {

    private final long delay;

    public FixedDelay(WarpTaskExecutingScheduledExecutorService executorService, final Runnable command, final long initialDelay, final long delay,
                      final TimeUnit unit) {
        super(executorService, command, initialDelay, unit);
        this.delay = delay;
    }

    @Override
    void updateNextRuntime() {
        nextRuntime = executorService.getClock().millis() + unit.toMillis(delay);
    }
}
