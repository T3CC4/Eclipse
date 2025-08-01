package de.tecca.eclipse.core;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ShutdownManager {

    private final List<ShutdownTask> shutdownTasks = new CopyOnWriteArrayList<>();
    private volatile boolean shuttingDown = false;

    public void addShutdownTask(String name, Runnable task) {
        addShutdownTask(name, task, ShutdownPriority.NORMAL);
    }

    public void addShutdownTask(String name, Runnable task, ShutdownPriority priority) {
        shutdownTasks.add(new ShutdownTask(name, task, priority));
        shutdownTasks.sort((a, b) -> Integer.compare(b.priority.getLevel(), a.priority.getLevel()));
    }

    public void executeShutdown() {
        if (shuttingDown) {
            return;
        }

        shuttingDown = true;

        CountDownLatch latch = new CountDownLatch(shutdownTasks.size());

        for (ShutdownTask task : shutdownTasks) {
            try {
                System.out.println("Executing shutdown task: " + task.name);
                task.task.run();
            } catch (Exception e) {
                System.err.println("Error in shutdown task " + task.name + ": " + e.getMessage());
            } finally {
                latch.countDown();
            }
        }

        try {
            if (!latch.await(30, TimeUnit.SECONDS)) {
                System.err.println("Shutdown tasks did not complete within 30 seconds");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Shutdown completed");
    }

    public boolean isShuttingDown() {
        return shuttingDown;
    }

    public int getTaskCount() {
        return shutdownTasks.size();
    }

    private static class ShutdownTask {
        final String name;
        final Runnable task;
        final ShutdownPriority priority;

        ShutdownTask(String name, Runnable task, ShutdownPriority priority) {
            this.name = name;
            this.task = task;
            this.priority = priority;
        }
    }

    public enum ShutdownPriority {
        HIGHEST(4),
        HIGH(3),
        NORMAL(2),
        LOW(1),
        LOWEST(0);

        private final int level;

        ShutdownPriority(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }
}