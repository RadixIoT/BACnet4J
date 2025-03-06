package com.serotonin.warp;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Terry Packer
 */
public class TestingWarpScheduledExecutorService extends WarpScheduledExecutorService {

    private final Set<Runnable> runnables;
    private final Set<Callable<?>> callables;

    public TestingWarpScheduledExecutorService(final Clock clock) {
        super(clock);
        this.runnables = ConcurrentHashMap.newKeySet();
        this.callables = ConcurrentHashMap.newKeySet();
    }

    public TestingWarpScheduledExecutorService(final Clock clock,
        final ExecutorService executorService) {
        super(clock, executorService);
        this.runnables = ConcurrentHashMap.newKeySet();
        this.callables = ConcurrentHashMap.newKeySet();
    }

    @Override
    protected void executeInExecutor(Runnable command) {
        super.executeInExecutor(wrapRunnable(command));
    }

    @Override
    protected Future<?> submitToExecutor(Runnable runnable) {
        return super.submitToExecutor(wrapRunnable(runnable));
    }

    @Override
    protected <T> Future<T> submitToExecutor(Runnable task, T result) {
        return super.submitToExecutor(wrapRunnable(task), result);
    }

    @Override
    protected <V> Future<V> submitToExecutor(Callable<V> callable) {
        return super.submitToExecutor(wrapCallable(callable));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
        throws InterruptedException {
        return super.invokeAll(tasks.stream().map(this::wrapCallable).toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
        long timeout, TimeUnit unit) throws InterruptedException {
        return super.invokeAll(tasks.stream().map(this::wrapCallable).toList(), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
        TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return super.invokeAny(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
        throws InterruptedException, ExecutionException {
        return super.invokeAny(tasks);
    }

    private <V> TrackedCallable<V> wrapCallable(Callable<V> callable) {
        return new TrackedCallable<>(callable);
    }

    private TrackedRunnable wrapRunnable(Runnable runnable) {
        return new TrackedRunnable(runnable);
    }

    /**
     * Wait for all tasks to complete, waiting for a period of time, count times.
     *
     * @param period
     * @param timeUnit
     * @param periodsToWait
     * @param waitForCallables
     * @param waitForRunnables
     * @param waitForScheduledFutures
     * @return true if all tasks exited, false if not
     * @throws InterruptedException
     */
    public boolean waitForExecutorTasks(int period, TimeUnit timeUnit, int periodsToWait,
        boolean waitForCallables, boolean waitForRunnables, boolean waitForScheduledFutures)
        throws InterruptedException {
        int waits = 0;
        boolean callablesDone = !waitForCallables;
        boolean runnablesDone = !waitForRunnables;
        boolean scheduledFuturesDone = !waitForScheduledFutures;

        while (waits < periodsToWait) {
            if (waitForCallables) {
                synchronized (callables) {
                    if (callables.isEmpty()) {
                        callablesDone = true;
                    }
                }
            }
            if (waitForRunnables) {
                synchronized (runnables) {
                    if (runnables.isEmpty()) {
                        runnablesDone = true;
                    }
                }
            }
            if (waitForScheduledFutures) {
                if (tasks.isEmpty()) {
                    scheduledFuturesDone = true;
                }
            }

            if (!callablesDone || !runnablesDone || !scheduledFuturesDone) {
                timeUnit.sleep(period);
            } else {
                return true;
            }
            waits++;
        }
        return false;
    }

    class TrackedCallable<V> implements Callable<V> {

        private final Callable<V> callable;

        public TrackedCallable(Callable<V> callable) {
            this.callable = callable;
            callables.add(callable);
        }

        @Override
        public V call() throws Exception {
            try {
                return this.callable.call();
            } finally {
                callables.remove(callable);
            }
        }
    }

    class TrackedRunnable implements Runnable {

        private final Runnable runnable;

        public TrackedRunnable(Runnable runnable) {
            this.runnable = runnable;
            runnables.add(runnable);
        }

        @Override
        public void run() {
            try {
                this.runnable.run();
            } finally {
                runnables.remove(runnable);
            }
        }
    }
}
