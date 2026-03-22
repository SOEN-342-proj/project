package repository;

import model.SearchCriteria;
import model.Task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** In-memory store for tasks. */
public class TaskRepository {
    private final List<Task> tasks = new ArrayList<>();
    private int nextId = 1;

    public int nextId() { return nextId++; }

    public Task save(Task task) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getTaskId() == task.getTaskId()) {
                tasks.set(i, task);
                return task;
            }
        }
        tasks.add(task);
        return task;
    }

    public Task findById(int id) {
        for (Task t : tasks) {
            if (t.getTaskId() == id) return t;
        }
        return null;
    }

    public List<Task> findAllOpen() {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (t.getStatus() == model.enums.Status.OPEN) result.add(t);
        }
        Collections.sort(result, new Comparator<Task>() {
            public int compare(Task a, Task b) {
                if (a.getDueDate() == null) return 1;
                if (b.getDueDate() == null) return -1;
                return a.getDueDate().compareTo(b.getDueDate());
            }
        });
        return result;
    }

    public List<Task> findByCriteria(SearchCriteria criteria) {
        List<Task> result = new ArrayList<>();
        for (Task t : tasks) {
            if (criteria.getStatus() != null && t.getStatus() != criteria.getStatus()) continue;
            if (criteria.getNameMatch() != null && !criteria.getNameMatch().trim().isEmpty()) {
                if (!t.getTitle().toLowerCase().contains(criteria.getNameMatch().toLowerCase())) continue;
            }
            if (criteria.getPeriodStart() != null && t.getDueDate() != null
                    && t.getDueDate().isBefore(criteria.getPeriodStart())) continue;
            if (criteria.getPeriodEnd() != null && t.getDueDate() != null
                    && t.getDueDate().isAfter(criteria.getPeriodEnd())) continue;
            if (criteria.getDayOfWeek() != null && t.getDueDate() != null
                    && t.getDueDate().getDayOfWeek() != criteria.getDayOfWeek()) continue;
            result.add(t);
        }
        Collections.sort(result, new Comparator<Task>() {
            public int compare(Task a, Task b) {
                if (a.getDueDate() == null) return 1;
                if (b.getDueDate() == null) return -1;
                return a.getDueDate().compareTo(b.getDueDate());
            }
        });
        return result;
    }

    public List<Task> findAll(SearchCriteria filter) {
        if (filter == null || filter.isEmpty()) return findAllOpen();
        return findByCriteria(filter);
    }

    public List<Task> getAll() { return new ArrayList<>(tasks); }
}