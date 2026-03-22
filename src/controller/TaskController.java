package controller;

import model.*;
import repository.CollaboratorRepository;
import repository.TaskRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Handles task-related business logic. */
public class TaskController {
    private final TaskRepository taskRepo;
    private final CollaboratorRepository collaboratorRepo;
    private final List<TaskActivity> activityLog = new ArrayList<>();
    private int nextSubtaskId = 1;
    private int nextActivityId = 1;

    public TaskController(TaskRepository taskRepo, CollaboratorRepository collaboratorRepo) {
        this.taskRepo = taskRepo;
        this.collaboratorRepo = collaboratorRepo;
    }

    public int createTask(String title, String description, model.enums.Priority priority, LocalDate dueDate) {
        int id = taskRepo.nextId();
        Task task = new Task(id, title, description, priority, dueDate);
        taskRepo.save(task);
        log(id, "Task created: " + title);
        return id;
    }

    public void updateTask(int taskId, String title, String description, model.enums.Priority priority, LocalDate dueDate) {
        Task task = taskRepo.findById(taskId);
        if (task == null) { System.out.println("Task not found: " + taskId); return; }
        if (title != null && !title.trim().isEmpty()) task.setTitle(title);
        if (description != null) task.setDescription(description);
        if (priority != null) task.setPriority(priority);
        if (dueDate != null) task.setDueDate(dueDate);
        log(taskId, "Task updated");
    }

    public void setStatus(int taskId, model.enums.Status status) {
        Task task = taskRepo.findById(taskId);
        if (task == null) { System.out.println("Task not found: " + taskId); return; }
        task.setStatus(status);
        log(taskId, "Status changed to " + status);
    }

    public boolean assignToProject(int taskId, Project project) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return false;
        task.assignToProject(project);
        log(taskId, "Assigned to project: " + project.getName());
        return true;
    }

    public int addSubtask(int taskId, String title) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return -1;
        int sid = nextSubtaskId++;
        task.addSubtask(new Subtask(sid, title));
        log(taskId, "Subtask added: " + title);
        return sid;
    }

    public void addTag(int taskId, String tagName) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return;
        task.addTag(new Tag(tagName));
    }

    public List<Task> listTasks(SearchCriteria filter) {
        return taskRepo.findAll(filter);
    }

    public List<TaskActivity> getTaskActivity(int taskId) {
        List<TaskActivity> result = new ArrayList<>();
        for (TaskActivity a : activityLog) {
            if (a.getTaskId() == taskId) result.add(a);
        }
        return result;
    }

    public Task getTask(int taskId) { return taskRepo.findById(taskId); }

    public TaskRepository getTaskRepo() { return taskRepo; }

    private void log(int taskId, String description) {
        activityLog.add(new TaskActivity(nextActivityId++, taskId, description));
    }
}