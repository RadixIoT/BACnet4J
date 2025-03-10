package com.serotonin.scheduler;

import java.time.Clock;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Terry Packer
 */
public interface TaskExecutingScheduledExecutorService extends ScheduledExecutorService {

    /**
     * Return a thread safe list of currently scheduled tasks
     * @return
     */
    List<ScheduleFutureImpl<?>> getTasks();
    <V> ScheduleFutureImpl<V> addTask(ScheduleFutureImpl<V> task);
    Clock getClock();
}
