package controller;

import model.Project;
import model.SearchCriteria;
import model.Task;
import repository.ProjectRepository;
import repository.TaskRepository;
import util.ICalGateway;

import java.util.List;
import java.util.stream.Collectors;

public class ICalExportController {

    private final TaskRepository taskRepo;
    private final ProjectRepository projectRepo;
    private final ICalGateway gateway;

    public ICalExportController(TaskRepository taskRepo, ProjectRepository projectRepo) {
        this.taskRepo    = taskRepo;
        this.projectRepo = projectRepo;
        this.gateway     = new ICalGateway();
    }

    public void exportTask(int taskId, String filePath) {
        Task task = taskRepo.findById(taskId);
        if (task == null) { System.out.println("Task not found: " + taskId); return; }
        if (task.getDueDate() == null) { System.out.println("Task has no due date — cannot export."); return; }
        gateway.exportToFile(List.of(task), filePath);
    }

    public void exportProject(String projectName, String filePath) {
        Project project = projectRepo.findByName(projectName);
        if (project == null) { System.out.println("Project not found: " + projectName); return; }

        List<Task> all = taskRepo.getAll();
        List<Task> projectTasks = all.stream()
                .filter(t -> t.getProject() != null &&
                        t.getProject().getProjectId() == project.getProjectId())
                .collect(Collectors.toList());

        if (projectTasks.isEmpty()) { System.out.println("No tasks found for project: " + projectName); return; }
        gateway.exportToFile(projectTasks, filePath);
    }

    public void exportFiltered(SearchCriteria criteria, String filePath) {
        List<Task> tasks = taskRepo.findAll(criteria);
        if (tasks.isEmpty()) { System.out.println("No tasks match the filter."); return; }
        gateway.exportToFile(tasks, filePath);
    }
}