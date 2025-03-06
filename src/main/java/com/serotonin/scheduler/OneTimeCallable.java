package com.serotonin.scheduler;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class OneTimeCallable<V> extends ScheduleFutureImpl<V> {
    protected final ExecutorService executorService;
    private final Callable<V> command;
    private final long runtime;

    public OneTimeCallable(TaskExecutingScheduledExecutorService scheduledExecutorService, final ExecutorService executorService, final Callable<V> command, final long delay, final TimeUnit unit) {
        super(scheduledExecutorService);
        this.executorService = executorService;
        this.command = command;
        runtime = scheduledExecutorService.getClock().millis() + unit.toMillis(delay);
    }

    @Override
    void execute() {
        setFuture(executorService.submit(command));
    }

    @Override
    public boolean isDone() {
        synchronized (this) {
            if (future != null)
                return future.isDone();
            return isCancelled();
        }
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final long millis = runtime - scheduledExecutorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }
}
