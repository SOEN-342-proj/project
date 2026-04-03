package repository;

import model.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectRepository {
    private final Connection conn;

    public ProjectRepository(Connection conn) { this.conn = conn; }

    public int nextId() {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(project_id),0)+1 FROM projects")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Project save(Project p) {
        try {
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM projects WHERE project_id=?")) {
                ps.setInt(1, p.getProjectId());
                exists = ps.executeQuery().next();
            }
            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE projects SET name=?,description=? WHERE project_id=?")) {
                    ps.setString(1, p.getName());
                    ps.setString(2, p.getDescription());
                    ps.setInt(3, p.getProjectId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO projects(project_id,name,description) VALUES(?,?,?)")) {
                    ps.setInt(1, p.getProjectId());
                    ps.setString(2, p.getName());
                    ps.setString(3, p.getDescription());
                    ps.executeUpdate();
                }
            }
            return p;
        } catch (SQLException e) { throw new RuntimeException("save project failed: " + e.getMessage(), e); }
    }

    public Project findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM projects WHERE project_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rowToProject(rs) : null;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Project findByName(String name) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM projects WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rowToProject(rs) : null;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Project> getAll() {
        List<Project> out = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM projects ORDER BY project_id")) {
            while (rs.next()) out.add(rowToProject(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    private Project rowToProject(ResultSet rs) throws SQLException {
        return new Project(rs.getInt("project_id"), rs.getString("name"), rs.getString("description"));
    }
}