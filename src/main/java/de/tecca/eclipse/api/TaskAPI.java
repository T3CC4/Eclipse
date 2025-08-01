package de.tecca.eclipse.api;

import de.tecca.eclipse.api.tasks.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public interface TaskAPI {

    void runSync(Runnable task);
    void runSyncLater(Runnable task, long delayTicks);
    int runSyncTimer(Runnable task, long delayTicks, long periodTicks);

    CompletableFuture<Void> runAsync(Runnable task);
    CompletableFuture<Void> runAsyncLater(Runnable task, long delay, TimeUnit unit);
    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);
    CompletableFuture<Void> runAsyncTimer(Runnable task, long delay, long period, TimeUnit unit);

    TaskBuilder queue();

    void cancelTask(int taskId);
    void cancelAllTasks();

    int getActiveTaskCount();
    int getQueuedTaskCount();

    void shutdown();
}