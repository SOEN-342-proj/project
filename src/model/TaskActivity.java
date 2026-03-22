package model;

import java.time.LocalDateTime;

public class TaskActivity {
    private int activityId;
    private int taskId;
    private LocalDateTime timestamp;
    private String description;

    public TaskActivity(int activityId, int taskId, String description) {
        this.activityId = activityId;
        this.taskId = taskId;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    public int getActivityId() { return activityId; }
    public int getTaskId() { return taskId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }
}