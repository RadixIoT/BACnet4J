package com.serotonin.warp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author Terry Packer
 */
public class OrderedExecutorService extends ThreadPoolExecutor {

    /**
     * Lock held on access to workers set and related bookkeeping. While we could use a concurrent
     * set of some sort, it turns out to be generally preferable to use a lock. Among the reasons is
     * that this serializes interruptIdleWorkers, which avoids unnecessary interrupt storms,
     * especially during shutdown. Otherwise exiting threads would concurrently interrupt those that
     * have not yet interrupted. It also simplifies some of the associated statistics bookkeeping of
     * largestPoolSize etc. We also hold mainLock on shutdown and shutdownNow, for the sake of
     * ensuring workers set is stable while separately checking permission to interrupt and actually
     * interrupting.
     */
    private final ReentrantReadWriteLock mainLock = new ReentrantReadWriteLock();

    private OrderedRunnable first;

    public OrderedExecutorService(int corePoolSize, int maximumPoolSize, long keepAliveTime,
        TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    public void execute(Runnable command) {
        ReentrantReadWriteLock lock = mainLock;
        lock.writeLock().lock();
        try {
            if (first == null) {
                first = new OrderedRunnable(null, command);
            } else {
                first = new OrderedRunnable(first, command);
            }
        } finally {
            lock.writeLock().unlock();
        }
        super.execute(first);
    }

    private class OrderedRunnable implements Runnable {

        private final OrderedRunnable previous;
        private volatile CountDownLatch latch;
        private final Runnable command;
        private volatile boolean success = false;
        private volatile boolean done = false;

        public OrderedRunnable(OrderedRunnable previous, Runnable command) {
            this.previous = previous;
            this.command = command;
            this.latch = new CountDownLatch(1);
        }

        public void await() throws InterruptedException {
            latch.await();
        }

        @Override
        public void run() {
            try {
                command.run();
                success = true;
                if (previous != null) {
                    previous.await();
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                done = true;
                latch.countDown();
            }
        }
    }
}
