package util;

import model.Subtask;
import model.Task;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.util.RandomUidGenerator;
import net.fortuna.ical4j.util.UidGenerator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.util.List;

public class ICalGateway {
    public void exportToFile(List<Task> tasks, String filePath) {
        Calendar calendar = new Calendar();
        calendar.getProperties().add(new ProdId("-//SOEN342 Task Manager//EN"));
        calendar.getProperties().add(Version.VERSION_2_0);
        calendar.getProperties().add(CalScale.GREGORIAN);

        UidGenerator ug = new RandomUidGenerator();

        for (Task task : tasks) {
            // skip tasks without due date
            if (task.getDueDate() == null) continue;

            // Convert LocalDate to iCal4j Date
            java.util.Date utilDate = java.util.Date.from(task.getDueDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
            Date icalDate = new Date(utilDate);

            VEvent event = new VEvent(icalDate, task.getTitle());

            event.getProperties().add(ug.generateUid());

            // Description: task description + status + priority + project + subtask summary
            StringBuilder desc = new StringBuilder();
            if (task.getDescription() != null && !task.getDescription().isEmpty()) {
                desc.append(task.getDescription()).append("\n");
            }
            desc.append("Status: ").append(task.getStatus()).append("\n");
            desc.append("Priority: ").append(task.getPriority()).append("\n");
            if (task.getProject() != null) {
                desc.append("Project: ").append(task.getProject().getName()).append("\n");
            }
            if (!task.getSubtasks().isEmpty()) {
                desc.append("Subtasks: ");
                for (int i = 0; i < task.getSubtasks().size(); i++) {
                    Subtask s = task.getSubtasks().get(i);
                    desc.append(s.getTitle())
                            .append(" [").append(s.isCompleted() ? "done" : "open").append("]");
                    if (i < task.getSubtasks().size() - 1) desc.append(", ");
                }
            }

            event.getProperties().add(new Description(desc.toString()));

            calendar.getComponents().add(event);
        }

        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, fos);
            System.out.println("Exported to: " + filePath);
        } catch (IOException e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }
}