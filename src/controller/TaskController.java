package controller;

import model.*;
import repository.CollaboratorRepository;
import repository.TaskRepository;

import java.time.LocalDate;
import java.util.List;

/** Handles task-related business logic. */
public class TaskController {
    private final TaskRepository taskRepo;
    private final CollaboratorRepository collaboratorRepo;

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
        taskRepo.save(task);
        log(taskId, "Task updated");
    }

    public void setStatus(int taskId, model.enums.Status status) {
        Task task = taskRepo.findById(taskId);
        if (task == null) { System.out.println("Task not found: " + taskId); return; }
        model.enums.Status prev = task.getStatus();
        task.setStatus(status);

        if (task.getCollaborator() != null) {
            Collaborator c = task.getCollaborator();
            if (prev != model.enums.Status.OPEN && status == model.enums.Status.OPEN) {
                c.incrementOpenTaskCount();
                if (!c.canAcceptTask()) {
					System.out.println("Warning: " + c.getName() + " is now overloaded.");
				}
            } else if (prev == model.enums.Status.OPEN && status != model.enums.Status.OPEN) {
                c.decrementOpenTaskCount();
            }
            collaboratorRepo.save(c);
        }

        taskRepo.save(task);
        log(taskId, "Status changed to " + status);
    }

    public boolean assignToProject(int taskId, Project project) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return false;
        task.assignToProject(project);
        taskRepo.save(task);
        log(taskId, "Assigned to project: " + project.getName());
        return true;
    }

    public int addSubtask(int taskId, String title) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return -1;
        int sid = taskRepo.nextSubtaskId();
        task.addSubtask(new Subtask(sid, title));
        taskRepo.save(task);
        log(taskId, "Subtask added: " + title);
        return sid;
    }

    public void addTag(int taskId, String tagName) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return;
        task.addTag(new Tag(tagName));
        taskRepo.save(task);
    }

    public List<Task> listTasks(SearchCriteria filter) {
        return taskRepo.findAll(filter);
    }

    public List<TaskActivity> getTaskActivity(int taskId) {
        return taskRepo.findActivitiesByTaskId(taskId);
    }

    public Task getTask(int taskId) { return taskRepo.findById(taskId); }

    public TaskRepository getTaskRepo() { return taskRepo; }

    public int createRecurringTask(String title, String description, model.enums.Priority priority,
                                   model.enums.RecurrenceType type, LocalDate startDate, LocalDate endDate,
                                   int interval, java.util.Set<java.time.DayOfWeek> weekdays, int dayOfMonth) {
        int taskId = createTask(title, description, priority, startDate);
        Task task = taskRepo.findById(taskId);

        RecurrencePattern rp = new RecurrencePattern(taskRepo.nextPatternId(), type, startDate, endDate, interval);
        if (weekdays != null) rp.setWeekdays(weekdays);
        rp.setDayOfMonth(dayOfMonth);
        task.setRecurrencePattern(rp);

        java.util.List<LocalDate> dates = rp.computeDueDates();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (LocalDate d : dates) {
            String key = title + "|" + d;
            if (seen.add(key)) task.addOccurrence(new TaskOccurrence(taskRepo.nextOccurrenceId(), taskId, d));
        }

        taskRepo.save(task);
        log(taskId, "Recurring task created with " + task.getOccurrences().size() + " occurrences");
        return taskId;
    }

    public boolean assignCollaborator(int taskId, int collaboratorId) {
        Task task = taskRepo.findById(taskId);
        if (task == null) return false;
        Collaborator c = collaboratorRepo.findById(collaboratorId);
        if (c == null) return false;
        if (!c.canAcceptTask()) {
            System.out.println("Cannot assign: " + c.getName() + " has reached the limit (" + c.getOpenTaskLimit() + ").");
            return false;
        }
        addSubtask(taskId, "Collaborated by: " + c.getName());
        task.setCollaborator(c);
        c.incrementOpenTaskCount();
        collaboratorRepo.save(c);
        taskRepo.save(task);
        log(taskId, "Collaborator assigned: " + c.getName());
        return true;
    }

    private void log(int taskId, String description) {
        TaskActivity a = new TaskActivity(taskRepo.nextActivityId(), taskId, description);
        taskRepo.saveActivity(a);
    }
}