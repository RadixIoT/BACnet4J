package com.serotonin.warp;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @author Terry Packer
 */
public class ObservableScheduledExecutorService implements ScheduledExecutorService {
    protected final WarpTaskExecutingScheduledExecutorService observedExecutorService;
    protected final Set<Runnable> runnables;
    protected final Set<Callable<?>> callables;
    protected final List<Consumer<Runnable>> runnableWatchers;
    protected final List<Consumer<Callable<?>>> callableWatchers;

    public ObservableScheduledExecutorService(final WarpTaskExecutingScheduledExecutorService observedExecutorService) {
        this.observedExecutorService = observedExecutorService;
        this.runnables = ConcurrentHashMap.newKeySet();
        this.callables = ConcurrentHashMap.newKeySet();
        this.runnableWatchers = new CopyOnWriteArrayList<>();
        this.callableWatchers = new CopyOnWriteArrayList<>();
    }

    /**
     * Add a watcher to be notified anytime a runnable is executed
     * @param runnableWatcher
     */
    public void addRunnableWatcher(final Consumer<Runnable> runnableWatcher) {
        this.runnableWatchers.add(runnableWatcher);
    }

    /**
     * Add a watcher to be notified anytime a callable is executed
     * @param callableWatcher
     */
    public void addCallableWatcher(final Consumer<Callable<?>> callableWatcher) {
        this.callableWatchers.add(callableWatcher);
    }

    @Override
    public void shutdown() {
        this.observedExecutorService.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return this.observedExecutorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.observedExecutorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.observedExecutorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.observedExecutorService.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return this.observedExecutorService.submit(wrapCallable(task));
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return this.observedExecutorService.submit(wrapRunnable(task), result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return this.observedExecutorService.submit(wrapRunnable(task));
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks)
            throws InterruptedException {
        return observedExecutorService.invokeAll(tasks.stream().map(this::wrapCallable).toList());
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks,
                                         long timeout, TimeUnit unit) throws InterruptedException {
        return observedExecutorService.invokeAll(tasks.stream().map(this::wrapCallable).toList(), timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout,
                           TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        //TODO These are not observed yet
        return observedExecutorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
            throws InterruptedException, ExecutionException {
        //TODO These are not observed yet
        return observedExecutorService.invokeAny(tasks);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        //Don't wrap these they get added ask tasks in the parent
        return this.observedExecutorService.schedule(command, delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        //Don't wrap these they get added ask tasks in the parent
        return this.observedExecutorService.schedule(callable, delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        //Don't wrap these they get added ask tasks in the parent
        return this.observedExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        //Don't wrap these they get added ask tasks in the parent
        return this.observedExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    @Override
    public void execute(Runnable command) {
        this.observedExecutorService.execute(wrapRunnable(command));
    }

    protected <V> TrackedCallable<V> wrapCallable(Callable<V> callable) {
        return new TrackedCallable<>(callable);
    }

    protected TrackedRunnable wrapRunnable(Runnable runnable) {
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
                if (observedExecutorService.getTasks().isEmpty()) {
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
                V result = this.callable.call();
                for(Consumer<Callable<?>> callableWatcher : callableWatchers) {
                    callableWatcher.accept(callable);
                }
                return result;
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
                for(Consumer<Runnable> runnableWatcher : runnableWatchers) {
                    runnableWatcher.accept(runnable);
                }
            } finally {
                runnables.remove(runnable);
            }
        }
    }
}
