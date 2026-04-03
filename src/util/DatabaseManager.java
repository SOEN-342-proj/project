package util;

import java.sql.*;

/**
 * Manages the SQLite connection and creates all tables on first run.
 * DB file: taskmanager.db (created next to wherever you run the jar).
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:taskmanager.db";
    private final Connection connection;

    public DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA foreign_keys = ON");
            }
            initSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialise database: " + e.getMessage(), e);
        }
    }

    public Connection getConnection() { return connection; }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ignored) {}
    }

    // ── DDL ───────────────────────────────────────────────────────────────
    private void initSchema() throws SQLException {
        String[] ddl = {

                "CREATE TABLE IF NOT EXISTS projects (" +
                        "  project_id   INTEGER PRIMARY KEY," +
                        "  name         TEXT NOT NULL UNIQUE," +
                        "  description  TEXT" +
                        ")",

                "CREATE TABLE IF NOT EXISTS collaborators (" +
                        "  collaborator_id  INTEGER PRIMARY KEY," +
                        "  name             TEXT NOT NULL," +
                        "  category         TEXT NOT NULL DEFAULT 'JUNIOR'," +
                        "  open_task_limit  INTEGER NOT NULL DEFAULT 10," +
                        "  open_task_count  INTEGER NOT NULL DEFAULT 0" +
                        ")",

                "CREATE TABLE IF NOT EXISTS tasks (" +
                        "  task_id         INTEGER PRIMARY KEY," +
                        "  title           TEXT NOT NULL," +
                        "  description     TEXT," +
                        "  priority        TEXT NOT NULL," +
                        "  status          TEXT NOT NULL," +
                        "  due_date        TEXT," +
                        "  creation_date   TEXT NOT NULL," +
                        "  project_id      INTEGER REFERENCES projects(project_id)," +
                        "  collaborator_id INTEGER REFERENCES collaborators(collaborator_id)" +
                        ")",

                "CREATE TABLE IF NOT EXISTS subtasks (" +
                        "  subtask_id  INTEGER PRIMARY KEY," +
                        "  task_id     INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                        "  title       TEXT NOT NULL," +
                        "  completed   INTEGER NOT NULL DEFAULT 0" +
                        ")",

                "CREATE TABLE IF NOT EXISTS tags (" +
                        "  tag_id    INTEGER PRIMARY KEY," +
                        "  tag_name  TEXT NOT NULL UNIQUE" +
                        ")",

                "CREATE TABLE IF NOT EXISTS task_tags (" +
                        "  task_id  INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                        "  tag_id   INTEGER NOT NULL REFERENCES tags(tag_id)," +
                        "  PRIMARY KEY (task_id, tag_id)" +
                        ")",

                "CREATE TABLE IF NOT EXISTS recurrence_patterns (" +
                        "  pattern_id    INTEGER PRIMARY KEY," +
                        "  task_id       INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                        "  type          TEXT NOT NULL," +
                        "  interval_val  INTEGER NOT NULL DEFAULT 1," +
                        "  day_of_month  INTEGER NOT NULL DEFAULT 0," +
                        "  start_date    TEXT," +
                        "  end_date      TEXT," +
                        "  weekdays      TEXT" +
                        ")",

                "CREATE TABLE IF NOT EXISTS task_occurrences (" +
                        "  occurrence_id  INTEGER PRIMARY KEY," +
                        "  task_id        INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                        "  due_date       TEXT NOT NULL," +
                        "  status         TEXT NOT NULL DEFAULT 'OPEN'" +
                        ")",

                "CREATE TABLE IF NOT EXISTS task_activities (" +
                        "  activity_id  INTEGER PRIMARY KEY," +
                        "  task_id      INTEGER NOT NULL REFERENCES tasks(task_id) ON DELETE CASCADE," +
                        "  description  TEXT NOT NULL," +
                        "  timestamp    TEXT NOT NULL" +
                        ")"
        };

        try (Statement s = connection.createStatement()) {
            for (String sql : ddl) s.execute(sql);
        }
    }
}