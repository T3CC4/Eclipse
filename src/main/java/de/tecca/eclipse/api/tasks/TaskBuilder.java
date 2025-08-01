package de.tecca.eclipse.api.tasks;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface TaskBuilder {
    TaskBuilder priority(TaskPriority priority);
    TaskBuilder delay(long delay, TimeUnit unit);
    TaskBuilder repeat(long period, TimeUnit unit);
    TaskBuilder async();
    TaskBuilder sync();
    TaskBuilder onComplete(Consumer<TaskResult> callback);
    TaskBuilder onError(Consumer<Exception> errorHandler);

    CompletableFuture<TaskResult> execute(Runnable task);
    <T> CompletableFuture<T> execute(Supplier<T> supplier);
}