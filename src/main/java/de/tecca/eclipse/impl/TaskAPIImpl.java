package de.tecca.eclipse.impl;

import de.tecca.eclipse.api.TaskAPI;
import de.tecca.eclipse.api.tasks.*;
import de.tecca.eclipse.tasks.*;
import org.bukkit.plugin.Plugin;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TaskAPIImpl implements TaskAPI {

    private final Plugin plugin;
    private final TaskScheduler taskScheduler;
    private final Set<Integer> activeTasks = ConcurrentHashMap.newKeySet();

    public TaskAPIImpl(Plugin plugin) {
        this.plugin = plugin;
        this.taskScheduler = new TaskScheduler(plugin);
    }

    @Override
    public void runSync(Runnable task) {
        int taskId = plugin.getServer().getScheduler().runTask(plugin, task).getTaskId();
        activeTasks.add(taskId);
    }

    @Override
    public void runSyncLater(Runnable task, long delayTicks) {
        int taskId = plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks).getTaskId();
        activeTasks.add(taskId);
    }

    @Override
    public int runSyncTimer(Runnable task, long delayTicks, long periodTicks) {
        int taskId = plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks).getTaskId();
        activeTasks.add(taskId);
        return taskId;
    }

    @Override
    public CompletableFuture<Void> runAsync(Runnable task) {
        return CompletableFuture.runAsync(task);
    }

    @Override
    public CompletableFuture<Void> runAsyncLater(Runnable task, long delay, TimeUnit unit) {
        return taskScheduler.runAsyncLater(task, delay, unit);
    }

    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier);
    }

    @Override
    public CompletableFuture<Void> runAsyncTimer(Runnable task, long delay, long period, TimeUnit unit) {
        return taskScheduler.runAsyncTimer(task, delay, period, unit);
    }

    @Override
    public TaskBuilder queue() {
        return new TaskBuilderImpl(taskScheduler);
    }

    @Override
    public void cancelTask(int taskId) {
        plugin.getServer().getScheduler().cancelTask(taskId);
        activeTasks.remove(taskId);
    }

    @Override
    public void cancelAllTasks() {
        plugin.getServer().getScheduler().cancelTasks(plugin);
        activeTasks.clear();
        taskScheduler.cancelAllTasks();
    }

    @Override
    public int getActiveTaskCount() {
        return activeTasks.size();
    }

    @Override
    public int getQueuedTaskCount() {
        return taskScheduler.getQueuedTaskCount();
    }

    @Override
    public void shutdown() {
        cancelAllTasks();
        taskScheduler.shutdown();
    }
}