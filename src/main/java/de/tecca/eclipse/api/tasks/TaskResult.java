package de.tecca.eclipse.api.tasks;

public class TaskResult {
    private final boolean success;
    private final Exception exception;
    private final long executionTime;

    public TaskResult(boolean success, Exception exception, long executionTime) {
        this.success = success;
        this.exception = exception;
        this.executionTime = executionTime;
    }

    public boolean isSuccess() { return success; }
    public Exception getException() { return exception; }
    public long getExecutionTime() { return executionTime; }
}