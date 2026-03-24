package model;

import model.enums.Status;
import java.time.LocalDate;

public class TaskOccurrence {
    private int occurrenceId;
    private int taskId;
    private LocalDate dueDate;
    private Status status;

    public TaskOccurrence(int occurrenceId, int taskId, LocalDate dueDate) {
        this.occurrenceId = occurrenceId;
        this.taskId = taskId;
        this.dueDate = dueDate;
        this.status = Status.OPEN;
    }

    public int getOccurrenceId() { return occurrenceId; }
    public int getTaskId() { return taskId; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isOpen() { return status == Status.OPEN; }
}