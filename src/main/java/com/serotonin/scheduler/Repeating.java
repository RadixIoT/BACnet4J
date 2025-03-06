package com.serotonin.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
abstract class Repeating extends ScheduleFutureImpl<Void> {
    protected final ExecutorService executorService;
    protected final TimeUnit unit;
    private final Runnable command;
    protected long nextRuntime;

    public Repeating(TaskExecutingScheduledExecutorService scheduledExecutorService, ExecutorService executorService, final Runnable command, final long initialDelay, final TimeUnit unit) {
        super(scheduledExecutorService);
        this.command = () -> {
            command.run();
            synchronized (this) {
                if (!isCancelled()) {
                    // Reschedule to run at the period from the last run.
                    updateNextRuntime();
                    clearFuture();
                    scheduledExecutorService.addTask(this);
                }
            }
        };
        this.executorService = executorService;
        nextRuntime = scheduledExecutorService.getClock().millis() + unit.toMillis(initialDelay);
        this.unit = unit;
    }

    @SuppressWarnings("unchecked")
    @Override
    void execute() {
        synchronized (this) {
            setFuture((Future<Void>) executorService.submit(command));
        }
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final long millis = nextRuntime - scheduledExecutorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isDone() {
        return isCancelled();
    }

    abstract void updateNextRuntime();
}
