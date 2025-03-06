package com.serotonin.warp;

import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class OneTime extends ScheduleFutureImpl<Void> {

    private final Runnable command;
    private final long runtime;

    public OneTime(WarpTaskExecutingScheduledExecutorService executorService, final Runnable command, final long delay, final TimeUnit unit) {
        super(executorService);
        this.command = command;
        runtime = executorService.getClock().millis() + unit.toMillis(delay);
    }

    @Override
    Runnable getRunnable() {
        return command;
    }

    @Override
    void executeImpl() {
        command.run();
        success(null);
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final long millis = runtime - executorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }
}
