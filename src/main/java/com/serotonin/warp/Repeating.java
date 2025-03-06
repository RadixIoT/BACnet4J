package com.serotonin.warp;

import java.util.concurrent.TimeUnit;

/**
 * @author Terry Packer
 */
abstract class Repeating extends ScheduleFutureImpl<Void> {

    private final Runnable command;
    protected final TimeUnit unit;

    protected long nextRuntime;

    public Repeating(WarpTaskExecutingScheduledExecutorService executorService, final Runnable command, final long initialDelay, final TimeUnit unit) {
        super(executorService);
        this.command = () -> {
            command.run();
            if (!isCancelled()) {
                // Reschedule to run at the period from the last run.
                updateNextRuntime();
                executorService.addTask(this);
            }
        };
        nextRuntime = executorService.getClock().millis() + unit.toMillis(initialDelay);
        this.unit = unit;
    }

    @Override
    Runnable getRunnable() {
        return command;
    }

    @Override
    void executeImpl() {
        command.run();
    }

    @Override
    public long getDelay(final TimeUnit unit) {
        final long millis = nextRuntime - executorService.getClock().millis();
        return unit.convert(millis, TimeUnit.MILLISECONDS);
    }

    @Override
    public boolean isDone() {
        return isCancelled();
    }

    abstract void updateNextRuntime();
}
