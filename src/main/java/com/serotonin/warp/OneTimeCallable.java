package com.serotonin.warp;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class OneTimeCallable<V> extends ScheduleFutureImpl<V> {

    private final Callable<V> command;
    private final long runtime;

    public OneTimeCallable(WarpTaskExecutingScheduledExecutorService executorService, final Callable<V> command, final long delay, final TimeUnit unit) {
        super(executorService);
        this.command = command;
        runtime = executorService.getClock().millis() + unit.toMillis(delay);
    }

    @Override
    Runnable getRunnable() {
        return () -> {
            try {
                command.call();
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    void executeImpl() {
        try {
            success(command.call());
        } catch (final Exception e) {
            exception(e);
        }
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final long millis = runtime - executorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }
}
