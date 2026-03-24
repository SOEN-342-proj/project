package model;
import java.time.LocalDate;

/** Represents a subtask belonging to a task. */
public class Subtask {
    private int subtaskId;
    private String title;
    private boolean completionStatus;

    public Subtask(int subtaskId, String title) {
        this.subtaskId = subtaskId;
        this.title = title;
        this.completionStatus = false;
    }

    public int getSubtaskId() { return subtaskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completionStatus; }
    public void setCompletionStatus(boolean completed) { this.completionStatus = completed; }
}