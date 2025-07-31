package de.tecca.eclipse.queue;

import de.tecca.eclipse.Eclipse;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Professional task queue system for Eclipse Framework
 */
public class TaskQueue {

    private final Eclipse plugin;
    private final ConcurrentLinkedQueue<QueuedTask> taskQueue;
    private final AtomicBoolean running;
    private BukkitTask processorTask;

    public TaskQueue(Eclipse plugin) {
        this.plugin = plugin;
        this.taskQueue = new ConcurrentLinkedQueue<>();
        this.running = new AtomicBoolean(true);
        startProcessor();
    }

    /**
     * Add a task to the queue
     * @param task The task to add
     */
    public void addTask(Task task) {
        addTask(task, 0L);
    }

    /**
     * Add a task to the queue with delay
     * @param task The task to add
     * @param delayTicks Delay in ticks before execution
     */
    public void addTask(Task task, long delayTicks) {
        long executeAt = System.currentTimeMillis() + (delayTicks * 50); // Convert ticks to milliseconds
        taskQueue.offer(new QueuedTask(task, executeAt));
    }

    /**
     * Add a task to be executed asynchronously
     * @param task The task to add
     */
    public void addAsyncTask(Task task) {
        addAsyncTask(task, 0L);
    }

    /**
     * Add a task to be executed asynchronously with delay
     * @param task The task to add
     * @param delayTicks Delay in ticks before execution
     */
    public void addAsyncTask(Task task, long delayTicks) {
        long executeAt = System.currentTimeMillis() + (delayTicks * 50);
        taskQueue.offer(new QueuedTask(task, executeAt, true));
    }

    /**
     * Get the current queue size
     * @return Number of tasks in queue
     */
    public int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * Clear all tasks from the queue
     */
    public void clearQueue() {
        taskQueue.clear();
    }

    /**
     * Check if the queue is running
     * @return true if running
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Start the task processor
     */
    private void startProcessor() {
        processorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::processQueue, 1L, 1L);
    }

    /**
     * Process the task queue
     */
    private void processQueue() {
        if (!running.get()) return;

        long currentTime = System.currentTimeMillis();
        QueuedTask task;

        while ((task = taskQueue.peek()) != null && task.getExecuteAt() <= currentTime) {
            task = taskQueue.poll();
            if (task != null) {
                executeTask(task);
            }
        }
    }

    /**
     * Execute a queued task
     * @param queuedTask The task to execute
     */
    private void executeTask(QueuedTask queuedTask) {
        try {
            if (queuedTask.isAsync()) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    try {
                        queuedTask.getTask().execute();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            } else {
                queuedTask.getTask().execute();
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error executing queued task: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Shutdown the task queue
     */
    public void shutdown() {
        plugin.getLogger().info("Shutting down Task Queue...");

        // Stop accepting new tasks
        running.set(false);

        // Cancel the processor task
        if (processorTask != null && !processorTask.isCancelled()) {
            processorTask.cancel();
            plugin.getLogger().info("Task processor cancelled");
        }

        // Try to execute remaining tasks with timeout
        long shutdownStart = System.currentTimeMillis();
        int remainingTasks = taskQueue.size();

        if (remainingTasks > 0) {
            plugin.getLogger().info("Executing " + remainingTasks + " remaining tasks...");

            // Execute remaining tasks with 5 second timeout
            while (!taskQueue.isEmpty() && (System.currentTimeMillis() - shutdownStart) < 5000) {
                QueuedTask task = taskQueue.poll();
                if (task != null) {
                    try {
                        task.getTask().execute();
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error executing remaining task during shutdown: " + e.getMessage());
                    }
                }
            }
        }

        // Clear any remaining tasks
        int clearedTasks = taskQueue.size();
        clearQueue();

        if (clearedTasks > 0) {
            plugin.getLogger().warning("Cleared " + clearedTasks + " unfinished tasks during shutdown");
        }

        plugin.getLogger().info("Task Queue shutdown complete");
    }

    /**
     * Internal class for queued tasks
     */
    private static class QueuedTask {
        private final Task task;
        private final long executeAt;
        private final boolean async;

        public QueuedTask(Task task, long executeAt) {
            this(task, executeAt, false);
        }

        public QueuedTask(Task task, long executeAt, boolean async) {
            this.task = task;
            this.executeAt = executeAt;
            this.async = async;
        }

        public Task getTask() {
            return task;
        }

        public long getExecuteAt() {
            return executeAt;
        }

        public boolean isAsync() {
            return async;
        }
    }
}