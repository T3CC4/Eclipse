package de.tecca.eclipse.queue;

/**
 * Interface for tasks that can be queued in Eclipse Framework
 */
@FunctionalInterface
public interface Task {

    /**
     * Execute the task
     * @throws Exception if an error occurs during execution
     */
    void execute() throws Exception;

    /**
     * Get the task name for debugging purposes
     * @return Task name
     */
    default String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Get the task priority (lower numbers = higher priority)
     * @return Priority value
     */
    default int getPriority() {
        return 100;
    }
}