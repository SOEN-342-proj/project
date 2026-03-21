package model;

import model.enums.Priority;
import model.enums.Status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Represents a task in the system. */
public class Task {
    private int taskId;
    private String title;
    private String description;
    private Priority priority;
    private LocalDate dueDate;
    private Status status;
    private LocalDate creationDate;
    private List<Subtask> subtasks;
    private List<Tag> tags;
    private Project project;
    private Collaborator collaborator;

    public Task(int taskId, String title, String description, Priority priority, LocalDate dueDate) {
        this.taskId = taskId;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = Status.OPEN;
        this.creationDate = LocalDate.now();
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
    }

    public int getTaskId() { return taskId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public LocalDate getCreationDate() { return creationDate; }

    public List<Subtask> getSubtasks() { return subtasks; }
    public void addSubtask(Subtask s) { subtasks.add(s); }
    public Subtask findSubtask(int subtaskId) {
        for (Subtask s : subtasks) {
            if (s.getSubtaskId() == subtaskId) return s;
        }
        return null;
    }

    public List<Tag> getTags() { return tags; }
    public void addTag(Tag t) { tags.add(t); }
    public void removeTag(Tag t) { tags.remove(t); }

    public Project getProject() { return project; }
    public void assignToProject(Project p) { this.project = p; }
    public void removeFromProject() { this.project = null; }

    public Collaborator getCollaborator() { return collaborator; }
    public void setCollaborator(Collaborator c) { this.collaborator = c; }

    @Override
    public String toString() {
        return String.format("[%d] %s | %s | %s | Due: %s | Project: %s",
                taskId, title, priority, status, dueDate,
                project != null ? project.getName() : "-");
    }
}