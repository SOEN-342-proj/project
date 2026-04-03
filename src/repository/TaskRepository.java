package repository;

import model.*;
import model.enums.*;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

/** SQLite-backed store for tasks and all child data. */
public class TaskRepository {

    private final Connection conn;

    public TaskRepository(Connection conn) { this.conn = conn; }

    public int nextId()           { return maxPlusOne("tasks",               "task_id"); }
    public int nextSubtaskId()    { return maxPlusOne("subtasks",            "subtask_id"); }
    public int nextOccurrenceId() { return maxPlusOne("task_occurrences",    "occurrence_id"); }
    public int nextActivityId()   { return maxPlusOne("task_activities",     "activity_id"); }
    public int nextPatternId()    { return maxPlusOne("recurrence_patterns", "pattern_id"); }

    private int maxPlusOne(String table, String col) {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(" + col + "),0)+1 FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── save ─────────────────────────────────────────────────────────────
    public Task save(Task task) {
        try {
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM tasks WHERE task_id=?")) {
                ps.setInt(1, task.getTaskId());
                exists = ps.executeQuery().next();
            }

            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE tasks SET title=?,description=?,priority=?,status=?, due_date=?,project_id=?,collaborator_id=? WHERE task_id=?")) {
                    ps.setString(1, task.getTitle());
                    ps.setString(2, task.getDescription());
                    ps.setString(3, task.getPriority().name());
                    ps.setString(4, task.getStatus().name());
                    ps.setString(5, task.getDueDate() != null ? task.getDueDate().toString() : null);
                    setNullableInt(ps, 6, task.getProject()      != null ? task.getProject().getProjectId()           : null);
                    setNullableInt(ps, 7, task.getCollaborator() != null ? task.getCollaborator().getCollaboratorId() : null);
                    ps.setInt(8, task.getTaskId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tasks(task_id,title,description,priority,status," +
                                "due_date,creation_date,project_id,collaborator_id) VALUES(?,?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, task.getTaskId());
                    ps.setString(2, task.getTitle());
                    ps.setString(3, task.getDescription());
                    ps.setString(4, task.getPriority().name());
                    ps.setString(5, task.getStatus().name());
                    ps.setString(6, task.getDueDate() != null ? task.getDueDate().toString() : null);
                    ps.setString(7, task.getCreationDate().toString());
                    setNullableInt(ps, 8, task.getProject()      != null ? task.getProject().getProjectId()           : null);
                    setNullableInt(ps, 9, task.getCollaborator() != null ? task.getCollaborator().getCollaboratorId() : null);
                    ps.executeUpdate();
                }
            }

            saveSubtasks(task);
            saveTags(task);
            saveRecurrence(task);
            saveOccurrences(task);
            return task;
        } catch (SQLException e) { throw new RuntimeException("save task failed: " + e.getMessage(), e); }
    }

    /** Persist an activity log entry. Called from TaskController after any state change. */
    public void saveActivity(TaskActivity a) {
        try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO task_activities(activity_id,task_id,description,timestamp) VALUES(?,?,?,?)")) {
            ps.setInt(1, a.getActivityId());
            ps.setInt(2, a.getTaskId());
            ps.setString(3, a.getDescription());
            ps.setString(4, a.getTimestamp().toString());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ── find ─────────────────────────────────────────────────────────────
    public Task findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM tasks WHERE task_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? hydrate(rs) : null;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Task> findAllOpen() {
        return query("SELECT * FROM tasks WHERE status='OPEN' ORDER BY CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date ASC");
    }

    public List<Task> findByCriteria(SearchCriteria c) {
        StringBuilder sql = new StringBuilder("SELECT * FROM tasks WHERE 1=1");
        List<Object> p   = new ArrayList<>();

        if (c.getStatus() != null) {
            sql.append(" AND status=?");
            p.add(c.getStatus().name());
        }
        if (c.getNameMatch() != null && !c.getNameMatch().trim().isEmpty()) {
            sql.append(" AND LOWER(title) LIKE ?");
            p.add("%" + c.getNameMatch().toLowerCase() + "%");
        }
        if (c.getPeriodStart() != null) { sql.append(" AND due_date >= ?"); p.add(c.getPeriodStart().toString()); }
        if (c.getPeriodEnd()   != null) { sql.append(" AND due_date <= ?"); p.add(c.getPeriodEnd().toString()); }
        if (c.getDayOfWeek()   != null) {
            // SQLite strftime('%w'): 0=Sunday … 6=Saturday
            int sqliteDow = c.getDayOfWeek() == DayOfWeek.SUNDAY ? 0 : c.getDayOfWeek().getValue();
            sql.append(" AND CAST(strftime('%w',due_date) AS INTEGER)=?");
            p.add(sqliteDow);
        }
        sql.append(" ORDER BY CASE WHEN due_date IS NULL THEN 1 ELSE 0 END, due_date ASC");

        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, p);
            return list(ps);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Task> findAll(SearchCriteria filter) {
        return (filter == null || filter.isEmpty()) ? findAllOpen() : findByCriteria(filter);
    }

    public List<Task> getAll() {
        return query("SELECT * FROM tasks ORDER BY task_id");
    }

    public List<TaskActivity> findActivitiesByTaskId(int taskId) {
        List<TaskActivity> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM task_activities WHERE task_id=? ORDER BY timestamp ASC")) {
            ps.setInt(1, taskId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                out.add(new TaskActivity(rs.getInt("activity_id"), rs.getInt("task_id"),
                        rs.getString("description")));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    private List<Task> query(String sql) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            return list(ps);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private List<Task> list(PreparedStatement ps) throws SQLException {
        List<Task> out = new ArrayList<>();
        ResultSet rs = ps.executeQuery();
        while (rs.next()) out.add(hydrate(rs));
        return out;
    }

    private Task hydrate(ResultSet rs) throws SQLException {
        int      id       = rs.getInt("task_id");
        String   title    = rs.getString("title");
        String   desc     = rs.getString("description");
        Priority priority = Priority.valueOf(rs.getString("priority"));
        String   dds      = rs.getString("due_date");
        LocalDate dd      = dds != null ? LocalDate.parse(dds) : null;
        int projectId      = rs.getInt("project_id");       // 0 if NULL
        int collaboratorId = rs.getInt("collaborator_id");  // 0 if NULL

        Task t = new Task(id, title, desc, priority, dd);
        t.setStatus(Status.valueOf(rs.getString("status")));

        if (projectId      > 0) loadProject(t, projectId);
        if (collaboratorId > 0) loadCollaborator(t, collaboratorId);
        loadSubtasks(t);
        loadTags(t);
        loadRecurrence(t);
        loadOccurrences(t);
        return t;
    }

    // ── load child data ───────────────────────────────────────────────────
    private void loadProject(Task t, int projectId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM projects WHERE project_id=?")) {
            ps.setInt(1, projectId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                t.assignToProject(new Project(
                        rs.getInt("project_id"), rs.getString("name"), rs.getString("description")));
            }
        }
    }

    private void loadCollaborator(Task t, int collaboratorId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM collaborators WHERE collaborator_id=?")) {
            ps.setInt(1, collaboratorId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) t.setCollaborator(buildCollaborator(rs));
        }
    }

    private void loadSubtasks(Task t) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM subtasks WHERE task_id=?")) {
            ps.setInt(1, t.getTaskId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Subtask s = new Subtask(rs.getInt("subtask_id"), rs.getString("title"));
                if (rs.getInt("completed") == 1) s.setCompletionStatus(true);
                t.addSubtask(s);
            }
        }
    }

    private void loadTags(Task t) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT tg.tag_name FROM tags tg JOIN task_tags tt ON tg.tag_id=tt.tag_id WHERE tt.task_id=?")) {
            ps.setInt(1, t.getTaskId());
            ResultSet rs = ps.executeQuery();
            // Assumes Tag(String tagName) — adjust if needed
            while (rs.next()) t.addTag(new Tag(rs.getString("tag_name")));
        }
    }

    private void loadRecurrence(Task t) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM recurrence_patterns WHERE task_id=?")) {
            ps.setInt(1, t.getTaskId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String sd = rs.getString("start_date"), ed = rs.getString("end_date");
                RecurrencePattern rp = new RecurrencePattern(
                        rs.getInt("pattern_id"),
                        RecurrenceType.valueOf(rs.getString("type")),
                        sd != null ? LocalDate.parse(sd) : null,
                        ed != null ? LocalDate.parse(ed) : null,
                        rs.getInt("interval_val"));
                rp.setDayOfMonth(rs.getInt("day_of_month"));
                String wds = rs.getString("weekdays");
                if (wds != null && !wds.isEmpty()) {
                    Set<DayOfWeek> days = new HashSet<>();
                    for (String d : wds.split(",")) days.add(DayOfWeek.valueOf(d.trim()));
                    rp.setWeekdays(days);
                }
                t.setRecurrencePattern(rp);
            }
        }
    }

    private void loadOccurrences(Task t) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM task_occurrences WHERE task_id=?")) {
            ps.setInt(1, t.getTaskId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TaskOccurrence o = new TaskOccurrence(
                        rs.getInt("occurrence_id"), t.getTaskId(),
                        LocalDate.parse(rs.getString("due_date")));
                o.setStatus(Status.valueOf(rs.getString("status")));
                t.addOccurrence(o);
            }
        }
    }

    // ── save child data ───────────────────────────────────────────────────
    private void saveSubtasks(Task task) throws SQLException {
        try (PreparedStatement d = conn.prepareStatement("DELETE FROM subtasks WHERE task_id=?")) {
            d.setInt(1, task.getTaskId()); d.executeUpdate();
        }
        for (Subtask s : task.getSubtasks()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO subtasks(subtask_id,task_id,title,completed) VALUES(?,?,?,?)")) {
                ps.setInt(1, s.getSubtaskId());
                ps.setInt(2, task.getTaskId());
                ps.setString(3, s.getTitle());
                ps.setInt(4, s.isCompleted() ? 1 : 0);
                ps.executeUpdate();
            }
        }
    }

    private void saveTags(Task task) throws SQLException {
        try (PreparedStatement d = conn.prepareStatement("DELETE FROM task_tags WHERE task_id=?")) {
            d.setInt(1, task.getTaskId()); d.executeUpdate();
        }
        for (Tag tag : task.getTags()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO tags(tag_name) VALUES(?)")) {
                ps.setString(1, tag.getTagName()); ps.executeUpdate();
            }
            int tagId;
            try (PreparedStatement ps = conn.prepareStatement("SELECT tag_id FROM tags WHERE tag_name=?")) {
                ps.setString(1, tag.getTagName());
                tagId = ps.executeQuery().getInt(1);
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO task_tags(task_id,tag_id) VALUES(?,?)")) {
                ps.setInt(1, task.getTaskId()); ps.setInt(2, tagId); ps.executeUpdate();
            }
        }
    }

    private void saveRecurrence(Task task) throws SQLException {
        try (PreparedStatement d = conn.prepareStatement("DELETE FROM recurrence_patterns WHERE task_id=?")) {
            d.setInt(1, task.getTaskId()); d.executeUpdate();
        }
        RecurrencePattern rp = task.getRecurrencePattern();
        if (rp == null) return;

        StringBuilder wds = new StringBuilder();
        for (DayOfWeek d : rp.getWeekdays()) {
            if (wds.length() > 0) wds.append(",");
            wds.append(d.name());
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO recurrence_patterns (pattern_id,task_id,type,interval_val,day_of_month,start_date,end_date,weekdays) VALUES(?,?,?,?,?,?,?,?)")) {
            ps.setInt(1, rp.getPatternId());
            ps.setInt(2, task.getTaskId());
            ps.setString(3, rp.getType().name());
            ps.setInt(4, rp.getInterval());
            ps.setInt(5, rp.getDayOfMonth());
            ps.setString(6, rp.getStartDate() != null ? rp.getStartDate().toString() : null);
            ps.setString(7, rp.getEndDate()   != null ? rp.getEndDate().toString()   : null);
            ps.setString(8, wds.toString());
            ps.executeUpdate();
        }
    }

    private void saveOccurrences(Task task) throws SQLException {
        try (PreparedStatement d = conn.prepareStatement("DELETE FROM task_occurrences WHERE task_id=?")) {
            d.setInt(1, task.getTaskId()); d.executeUpdate();
        }
        for (TaskOccurrence o : task.getOccurrences()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO task_occurrences(occurrence_id,task_id,due_date,status) VALUES(?,?,?,?)")) {
                ps.setInt(1, o.getOccurrenceId());
                ps.setInt(2, task.getTaskId());
                ps.setString(3, o.getDueDate().toString());
                ps.setString(4, o.getStatus().name());
                ps.executeUpdate();
            }
        }
    }

    private void setNullableInt(PreparedStatement ps, int i, Integer val) throws SQLException {
        if (val != null) ps.setInt(i, val); else ps.setNull(i, Types.INTEGER);
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if      (p instanceof String)  ps.setString(i + 1, (String)  p);
            else if (p instanceof Integer) ps.setInt   (i + 1, (Integer) p);
        }
    }

    private Collaborator buildCollaborator(ResultSet rs) throws SQLException {
        model.enums.Category cat = model.enums.Category.valueOf(rs.getString("category"));
        Collaborator c = new Collaborator(rs.getInt("collaborator_id"), rs.getString("name"), cat);
        c.setOpenTaskLimit(rs.getInt("open_task_limit"));
        c.setOpenTaskCount(rs.getInt("open_task_count"));
        return c;
    }
}