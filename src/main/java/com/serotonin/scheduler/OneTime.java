package com.serotonin.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
class OneTime extends ScheduleFutureImpl<Void> {
    private final ExecutorService executorService;
    private final Runnable command;
    private final long runtime;

    public OneTime(TaskExecutingScheduledExecutorService scheduledExecutorService, ExecutorService executorService, final Runnable command, final long delay, final TimeUnit unit) {
        super(scheduledExecutorService);
        this.executorService = executorService;
        this.command = command;
        runtime = scheduledExecutorService.getClock().millis() + unit.toMillis(delay);
    }

    @SuppressWarnings("unchecked")
    @Override
    void execute() {
        synchronized (this) {
            setFuture((Future<Void>) executorService.submit(command));
        }
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
