package repository;

import model.Collaborator;
import model.enums.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CollaboratorRepository {
    private final Connection conn;

    public CollaboratorRepository(Connection conn) { this.conn = conn; }

    public int nextId() {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(
                     "SELECT COALESCE(MAX(collaborator_id),0)+1 FROM collaborators")) {
            return rs.next() ? rs.getInt(1) : 1;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Collaborator save(Collaborator c) {
        try {
            boolean exists;
            try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM collaborators WHERE collaborator_id=?")) {
                ps.setInt(1, c.getCollaboratorId());
                exists = ps.executeQuery().next();
            }
            if (exists) {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE collaborators SET name=?,category=?,open_task_limit=?,open_task_count=? WHERE collaborator_id=?")) {
                    ps.setString(1, c.getName());
                    ps.setString(2, c.getCategory().name());
                    ps.setInt(3, c.getOpenTaskLimit());
                    ps.setInt(4, c.getOpenTaskCount());
                    ps.setInt(5, c.getCollaboratorId());
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement("INSERT INTO collaborators(collaborator_id,name,category,open_task_limit,open_task_count) VALUES(?,?,?,?,?)")) {
                    ps.setInt(1, c.getCollaboratorId());
                    ps.setString(2, c.getName());
                    ps.setString(3, c.getCategory().name());
                    ps.setInt(4, c.getOpenTaskLimit());
                    ps.setInt(5, c.getOpenTaskCount());
                    ps.executeUpdate();
                }
            }
            return c;
        } catch (SQLException e) { throw new RuntimeException("save collaborator failed: " + e.getMessage(), e); }
    }

    public Collaborator findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM collaborators WHERE collaborator_id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rowToCollaborator(rs) : null;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Collaborator findByName(String name) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM collaborators WHERE LOWER(name)=LOWER(?)")) {
            ps.setString(1, name);
            ResultSet rs = ps.executeQuery();
            return rs.next() ? rowToCollaborator(rs) : null;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Collaborator> getAll() {
        List<Collaborator> out = new ArrayList<>();
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM collaborators ORDER BY collaborator_id")) {
            while (rs.next()) out.add(rowToCollaborator(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    private Collaborator rowToCollaborator(ResultSet rs) throws SQLException {
        Category cat = Category.valueOf(rs.getString("category"));
        Collaborator c = new Collaborator(rs.getInt("collaborator_id"), rs.getString("name"), cat);
        c.setOpenTaskLimit(rs.getInt("open_task_limit"));
        c.setOpenTaskCount(rs.getInt("open_task_count"));
        return c;
    }
}