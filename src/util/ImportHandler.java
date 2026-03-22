package util;

import controller.ProjectController;
import controller.TaskController;
import model.Project;
import model.Subtask;
import model.Task;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class ImportHandler {

    private static final String[] HEADERS = {
            "TaskName", "Description", "Subtask", "Status", "Priority",
            "DueDate", "ProjectName", "ProjectDescription", "Collaborator", "CollaboratorCategory"
    };
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final TaskController taskController;
    private final ProjectController projectController;
    private int successCount = 0;
    private int errorCount = 0;
    private final List<String> errorLog = new ArrayList<>();

    public ImportHandler(TaskController taskController, ProjectController projectController) {
        this.taskController = taskController;
        this.projectController = projectController;
    }

    public ImportSummary importTasksFromCSV(File file) {
        successCount = 0;
        errorCount = 0;
        errorLog.clear();
        List<String[]> rows = parseRows(file);
        if (rows == null) return new ImportSummary(0, 1, errorLog);
        for (int i = 0; i < rows.size(); i++) {
            int rowNum = i + 2;
            try {
                processRow(rows.get(i));
                successCount++;
            } catch (Exception e) {
                logRowError(rowNum, e.getMessage());
            }
        }
        return new ImportSummary(successCount, errorCount, errorLog);
    }

    private void processRow(String[] row) {
        String taskName    = get(row, 0);
        String desc        = get(row, 1);
        String subtask     = get(row, 2);
        String statusStr   = get(row, 3);
        String priorityStr = get(row, 4);
        String dueDateStr  = get(row, 5);
        String projName    = get(row, 6);
        String projDesc    = get(row, 7);
        String collabName  = get(row, 8);
        String collabCat   = get(row, 9);

        if (taskName.trim().isEmpty()) throw new IllegalArgumentException("TaskName is required");

        model.enums.Priority priority = model.enums.Priority.fromString(priorityStr);
        model.enums.Status status     = model.enums.Status.fromString(statusStr);

        LocalDate dueDate = null;
        if (!dueDateStr.trim().isEmpty()) {
            try {
                dueDate = LocalDate.parse(dueDateStr, DATE_FMT);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid DueDate: " + dueDateStr);
            }
        }

        Project project = null;
        if (!projName.trim().isEmpty()) {
            project = projectController.getProjectByName(projName);
            if (project == null) {
                int pid = projectController.createProject(projName, projDesc);
                project = projectController.getProject(pid);
            }
        }

        int taskId = taskController.createTask(taskName, desc, priority, dueDate);
        taskController.setStatus(taskId, status);
        if (project != null) taskController.assignToProject(taskId, project);
        if (!subtask.trim().isEmpty()) taskController.addSubtask(taskId, subtask);
        if (!collabName.trim().isEmpty() && project != null) {
            model.enums.Category cat = model.enums.Category.fromString(collabCat);
            int cid = projectController.addCollaboratorToProject(project.getProjectId(), collabName, cat);
            taskController.assignCollaborator(taskId, cid);
        }
    }

    public void exportTasksToCSV(File file) {
        List<Task> tasks = taskController.getTaskRepo().getAll();
        try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
            pw.println(String.join(",", HEADERS));
            for (Task t : tasks) pw.println(taskToCSVLine(t));
            System.out.println("Exported " + tasks.size() + " task(s) to " + file.getName());
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }

    private String taskToCSVLine(Task t) {
        String subtaskStr = "";
        if (!t.getSubtasks().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Subtask s : t.getSubtasks()) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(s.getTitle());
            }
            subtaskStr = sb.toString();
        }
        String projName  = t.getProject() != null ? t.getProject().getName() : "";
        String projDesc  = t.getProject() != null ? t.getProject().getDescription() : "";
        String collab    = t.getCollaborator() != null ? t.getCollaborator().getName() : "";
        String collabCat = t.getCollaborator() != null ? t.getCollaborator().getCategory().name() : "";
        String dueDate   = t.getDueDate() != null ? t.getDueDate().format(DATE_FMT) : "";

        return escape(t.getTitle()) + "," +
                escape(t.getDescription()) + "," +
                escape(subtaskStr) + "," +
                t.getStatus() + "," +
                t.getPriority() + "," +
                dueDate + "," +
                escape(projName) + "," +
                escape(projDesc) + "," +
                escape(collab) + "," +
                collabCat;
    }

    public List<String[]> parseRows(File file) {
        List<String[]> rows = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            if (br.readLine() == null) return rows;
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty()) rows.add(parseCSVLine(line));
            }
        } catch (IOException e) {
            return null;
        }
        return rows;
    }

    private String[] parseCSVLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                fields.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        fields.add(sb.toString().trim());
        return fields.toArray(new String[0]);
    }

    private String get(String[] row, int i) {
        return (i < row.length && row[i] != null) ? row[i].trim() : "";
    }

    private String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private void logRowError(int row, String message) {
        errorCount++;
        String msg = "Row " + row + ": ERROR - " + message;
        errorLog.add(msg);
        System.out.println(msg);
    }

    public static class ImportSummary {
        public final int success;
        public final int errors;
        public final List<String> errorMessages;

        public ImportSummary(int success, int errors, List<String> errorMessages) {
            this.success = success;
            this.errors = errors;
            this.errorMessages = errorMessages;
        }

        @Override
        public String toString() {
            return "Import complete: " + success + " succeeded, " + errors + " failed.";
        }
    }
}