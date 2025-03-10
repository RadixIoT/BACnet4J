package com.serotonin.warp;

import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class FixedRate extends Repeating {

    private final long period;

    public FixedRate(WarpTaskExecutingScheduledExecutorService executorService, final Runnable command, final long initialDelay, final long period,
                     final TimeUnit unit) {
        super(executorService, command, initialDelay, unit);
        this.period = period;
    }

    @Override
    void updateNextRuntime() {
        nextRuntime += unit.toMillis(period);
    }
}
