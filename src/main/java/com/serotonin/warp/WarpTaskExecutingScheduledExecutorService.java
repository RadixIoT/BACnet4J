package com.serotonin.warp;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * TODO Combine/Replace with scheduler package impl
 * @author Terry Packer
 */
public interface WarpTaskExecutingScheduledExecutorService extends ScheduledExecutorService {
    /**
     * Return a thread safe list of currently scheduled tasks
     * @return
     */
    List<ScheduleFutureImpl<?>> getTasks();
    <V> ScheduleFutureImpl<V> addTask(ScheduleFutureImpl<V> task);
    Clock getClock();
}
