import controller.ICalExportController;
import controller.ProjectController;
import controller.TaskController;
import model.*;
import model.enums.*;
import repository.CollaboratorRepository;
import repository.ProjectRepository;
import repository.TaskRepository;
import util.DatabaseManager;
import util.ImportHandler;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TaskController taskController;
    private final ProjectController projectController;
    private final ICalExportController icalController;
    private final ImportHandler importHandler;
    private final DatabaseManager db;
    private final Scanner scanner = new Scanner(System.in);

    public Main() {
        // creates taskmanager.db + schema
        db = new DatabaseManager();
        java.sql.Connection conn = db.getConnection();
        TaskRepository taskRepo = new TaskRepository(conn);
        ProjectRepository projectRepo = new ProjectRepository(conn);
        CollaboratorRepository collaboratorRepo = new CollaboratorRepository(conn);
        taskController = new TaskController(taskRepo, collaboratorRepo);
        projectController = new ProjectController(projectRepo, collaboratorRepo);
        importHandler = new ImportHandler(taskController, projectController);
        icalController = new ICalExportController(taskRepo, projectController.getProjectRepo());
    }

    public static void main(String[] args) {
        new Main().run();
    }

    public void run() {
        System.out.println("====================================");
        System.out.println("       SOEN 342 Task Manager       ");
        System.out.println("====================================");
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Choice").trim();
            switch (choice) {
                case "1": handleSearchTasks();              break;
                case "2": handleCreateTask();               break;
                case "3": handleCreateProject();            break;
                case "4": handleImportCSV();                break;
                case "5": handleExportCSV();                break;
                case "6": handleViewTask();                 break;
                case "7": handleOverloadedCollaborators();  break;
                case "8": handleAddCollaboratorToProject(); break;
                case "9": handleAssignCollaboratorToTask(); break;
                case "10": handleICalExport(); break;
                case "0": running = false;                  break;
                default:  System.out.println("Invalid option.");
            }
        }
        System.out.println("Goodbye.");
        db.close();   // flush and close SQLite connection
    }

    private void printMainMenu() {
        System.out.println("\n------------------------------------");
        System.out.println(" 1. Search / List Tasks");
        System.out.println(" 2. Create Task");
        System.out.println(" 3. Create Project");
        System.out.println(" 4. Import Tasks from CSV");
        System.out.println(" 5. Export Tasks to CSV");
        System.out.println(" 6. View Task Details");
        System.out.println(" 7. View Overloaded Collaborators");
        System.out.println(" 8. Add Collaborator to Project");
        System.out.println(" 9. Assign Collaborator to Task");
        System.out.println(" 10. Export to iCal (.ics)");
        System.out.println(" 0. Exit");
        System.out.println("------------------------------------");
    }

    private void handleSearchTasks() {
        System.out.println("\n-- Search Tasks --");
        System.out.println("Leave blank to skip a filter.");
        SearchCriteria criteria = new SearchCriteria();

        String name = prompt("Task name contains");
        if (!name.trim().isEmpty()) criteria.setNameMatch(name);

        String statusStr = prompt("Status (OPEN/COMPLETED/CANCELLED)");
        if (!statusStr.trim().isEmpty()) criteria.setStatus(Status.fromString(statusStr));

        String from = prompt("Due date from (yyyy-MM-dd)");
        if (!from.trim().isEmpty()) {
            LocalDate d = parseDate(from);
            if (d != null) criteria.setPeriodStart(d);
        }

        String to = prompt("Due date to (yyyy-MM-dd)");
        if (!to.trim().isEmpty()) {
            LocalDate d = parseDate(to);
            if (d != null) criteria.setPeriodEnd(d);
        }

        String dow = prompt("Day of week (e.g. MONDAY)");
        if (!dow.trim().isEmpty()) {
            try {
                criteria.setDayOfWeek(DayOfWeek.valueOf(dow.trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid day, skipping.");
            }
        }

        printTaskList(taskController.listTasks(criteria), criteria.isEmpty() ? "All open tasks" : "Search results");
    }

    private void printTaskList(List<Task> tasks, String header) {
        System.out.println("\n-- " + header + " (" + tasks.size() + " found) --");
        if (tasks.isEmpty()) {
            System.out.println("  (no tasks found)");
            return;
        }
        System.out.printf("%-5s %-30s %-12s %-11s %-12s %-20s %-20s%n",
                "ID", "Title", "Priority", "Status", "Due Date", "Project", "Collaborator");
        System.out.println("-".repeat(115));
        for (Task t : tasks) {
            System.out.printf("%-5d %-30s %-12s %-11s %-12s %-20s %-20s%n",
                    t.getTaskId(),
                    truncate(t.getTitle(), 29),
                    t.getPriority(),
                    t.getStatus(),
                    t.getDueDate() != null ? t.getDueDate().format(DATE_FMT) : "-",
                    t.getProject() != null ? truncate(t.getProject().getName(), 19) : "-",
                    t.getCollaborator() != null ? truncate(t.getCollaborator().getName(), 19) : "-");
        }
    }

    private void handleCreateTask() {
        System.out.println("\n-- Create Task --");
        String title = prompt("Title (required)");
        if (title.trim().isEmpty()) { System.out.println("Title is required."); return; }

        String desc    = prompt("Description");
        String priStr  = prompt("Priority (LOW/MEDIUM/HIGH) [default: LOW]");
        String dateStr = prompt("Due date (yyyy-MM-dd) [optional]");

        Priority priority = Priority.fromString(priStr.trim().isEmpty() ? "LOW" : priStr);
        LocalDate dueDate = dateStr.trim().isEmpty() ? null : parseDate(dateStr);

        String projName = prompt("Project name (leave blank for none)");
        Project project = null;
        if (!projName.trim().isEmpty()) {
            project = projectController.getProjectByName(projName);
            if (project == null) {
                System.out.println("Project not found. Creating it.");
                String projDesc = prompt("Project description");
                int pid = projectController.createProject(projName, projDesc);
                project = projectController.getProject(pid);
            }
        }

        int taskId = taskController.createTask(title, desc, priority, dueDate);
        if (project != null) taskController.assignToProject(taskId, project);
        System.out.println("Task created with ID: " + taskId);

        String sub = prompt("Add a subtask? (title or blank to skip)");
        if (!sub.trim().isEmpty()) {
            int sid = taskController.addSubtask(taskId, sub);
            System.out.println("Subtask added with ID: " + sid);
        }

        System.out.println("\n  Task created successfully:");
        Task created = taskController.getTask(taskId);
        printTaskDetails(created);
    }

    private void handleCreateProject() {
        System.out.println("\n-- Create Project --");
        String name = prompt("Project name (required)");
        if (name.trim().isEmpty()) { System.out.println("Name is required."); return; }
        String desc = prompt("Description");
        int pid = projectController.createProject(name, desc);
        if (pid != -1) System.out.println("Project created with ID: " + pid);
    }

    private void handleImportCSV() {
        System.out.println("\n-- Import Tasks from CSV --");
        String path = prompt("File path (e.g. sample_tasks.csv)");
        if (path.trim().isEmpty()) { System.out.println("No path given."); return; }
        File file = new File(path);
        if (!file.exists()) { System.out.println("File not found: " + path); return; }
        ImportHandler.ImportSummary summary = importHandler.importTasksFromCSV(file);
        System.out.println(summary);
        if (!summary.errorMessages.isEmpty()) {
            System.out.println("Errors:");
            for (String e : summary.errorMessages) System.out.println("  " + e);
        }
    }

    private void handleExportCSV() {
        System.out.println("\n-- Export Tasks to CSV --");
        String path = prompt("Output file path [default: tasks_export.csv]");
        if (path.trim().isEmpty()) path = "tasks_export.csv";
        importHandler.exportTasksToCSV(new File(path));
    }

    private void handleViewTask() {
        System.out.println("\n-- View Task --");
        System.out.println("  Search by ID or name.");
        String input = prompt("Task ID or name");
        if (input.trim().isEmpty()) return;

        Task t = null;
        try {
            int id = Integer.parseInt(input.trim());
            t = taskController.getTask(id);
        } catch (NumberFormatException e) {
            SearchCriteria c = new SearchCriteria();
            c.setNameMatch(input);
            List<Task> results = taskController.listTasks(c);
            if (results.isEmpty()) {
                System.out.println("No task found matching: " + input);
                return;
            }
            if (results.size() > 1) System.out.println("Multiple matches — showing first result.");
            t = results.get(0);
        }

        if (t == null) { System.out.println("Task not found."); return; }
        printTaskDetails(t);
    }

    private void handleOverloadedCollaborators() {
        System.out.println("\n-- Overloaded Collaborators --");
        List<Collaborator> overloaded = projectController.getOverloadedCollaborators();
        if (overloaded.isEmpty()) {
            System.out.println("  No overloaded collaborators.");
            return;
        }
        System.out.printf("%-5s %-20s %-14s %-6s %-5s%n",
                "ID", "Name", "Category", "Limit", "Count");
        System.out.println("-".repeat(55));
        for (Collaborator c : overloaded) {
            System.out.printf("%-5d %-20s %-14s %-6d %-5d%n",
                    c.getCollaboratorId(), c.getName(),
                    c.getCategory(), c.getOpenTaskLimit(), c.getOpenTaskCount());
        }
    }

    private void handleAddCollaboratorToProject() {
        System.out.println("\n-- Add Collaborator to Project --");
        String projName = prompt("Project name");
        if (projName.trim().isEmpty()) return;
        Project project = projectController.getProjectByName(projName);
        if (project == null) { System.out.println("Project not found."); return; }

        String name = prompt("Collaborator name");
        if (name.trim().isEmpty()) return;
        String catStr = prompt("Category (JUNIOR/INTERMEDIATE/SENIOR) [default: JUNIOR]");
        Category category = Category.fromString(catStr.trim().isEmpty() ? "JUNIOR" : catStr);

        int cid = projectController.addCollaboratorToProject(project.getProjectId(), name, category);
        if (cid != -1) System.out.println("Collaborator added with ID: " + cid);
    }

    private void handleAssignCollaboratorToTask() {
        System.out.println("\n-- Assign Collaborator to Task --");
        String taskInput = prompt("Task ID");
        if (taskInput.trim().isEmpty()) return;
        int taskId;
        try { taskId = Integer.parseInt(taskInput.trim()); }
        catch (NumberFormatException e) { System.out.println("Invalid task ID."); return; }

        String collabInput = prompt("Collaborator ID");
        if (collabInput.trim().isEmpty()) return;
        int collabId;
        try { collabId = Integer.parseInt(collabInput.trim()); }
        catch (NumberFormatException e) { System.out.println("Invalid collaborator ID."); return; }

        boolean success = taskController.assignCollaborator(taskId, collabId);
        if (success) System.out.println("Collaborator assigned successfully.");
    }

    private void handleICalExport() {
        System.out.println("\n-- Export to iCal --");
        System.out.println("  1. Single task");
        System.out.println("  2. All tasks in a project");
        System.out.println("  3. Filtered tasks");
        String choice = prompt("Choice");
        String filePath = prompt("Output file path [default: export.ics]");
        if (filePath.trim().isEmpty()) filePath = "export.ics";

        switch (choice.trim()) {
            case "1":
                String tid = prompt("Task ID");
                try { icalController.exportTask(Integer.parseInt(tid.trim()), filePath); }
                catch (NumberFormatException e) { System.out.println("Invalid ID."); }
                break;
            case "2":
                String proj = prompt("Project name");
                icalController.exportProject(proj, filePath);
                break;
            case "3":
                SearchCriteria criteria = new SearchCriteria();
                String name = prompt("Task name contains (blank to skip)");
                if (!name.trim().isEmpty()) criteria.setNameMatch(name);
                String status = prompt("Status (OPEN/COMPLETED/CANCELLED) (blank to skip)");
                if (!status.trim().isEmpty()) criteria.setStatus(Status.fromString(status));
                icalController.exportFiltered(criteria, filePath);
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private void printTaskDetails(Task t) {
        System.out.println("\n  ID:          " + t.getTaskId());
        System.out.println("  Title:       " + t.getTitle());
        System.out.println("  Description: " + (t.getDescription() != null ? t.getDescription() : "-"));
        System.out.println("  Priority:    " + t.getPriority());
        System.out.println("  Status:      " + t.getStatus());
        System.out.println("  Due Date:    " + (t.getDueDate() != null ? t.getDueDate().format(DATE_FMT) : "-"));
        System.out.println("  Created:     " + t.getCreationDate().format(DATE_FMT));
        System.out.println("  Project:     " + (t.getProject() != null ? t.getProject().getName() : "-"));
        System.out.println("  Collaborator:" + (t.getCollaborator() != null ? t.getCollaborator().toString() : "-"));

        if (!t.getSubtasks().isEmpty()) {
            System.out.println("  Subtasks:");
            for (Subtask s : t.getSubtasks()) {
                System.out.println("    [" + s.getSubtaskId() + "] " + s.getTitle()
                        + (s.isCompleted() ? " (done)" : ""));
            }
        }

        if (!t.getTags().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Tag tag : t.getTags()) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(tag.getTagName());
            }
            System.out.println("  Tags:        " + sb);
        }

        if (t.getRecurrencePattern() != null) {
            RecurrencePattern rp = t.getRecurrencePattern();
            System.out.println("  Recurrence:  " + rp.getType()
                    + " from " + rp.getStartDate() + " to " + rp.getEndDate());
            System.out.println("  Occurrences: " + t.getOccurrences().size());
        }

        List<TaskActivity> activities = taskController.getTaskActivity(t.getTaskId());
        if (!activities.isEmpty()) {
            System.out.println("  Activity Log:");
            for (TaskActivity a : activities) {
                System.out.println("    " + a.getTimestamp() + " - " + a.getDescription());
            }
        }
    }

    private String prompt(String label) {
        System.out.print("  " + label + ": ");
        return scanner.nextLine();
    }

    private LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (DateTimeParseException e) {
            System.out.println("  Invalid date (expected yyyy-MM-dd): " + s);
            return null;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "...";
    }
}