package repository;

import model.Project;

import java.util.ArrayList;
import java.util.List;

/** In-memory store for projects. */
public class ProjectRepository {
    private final List<Project> projects = new ArrayList<>();
    private int nextId = 1;

    public int nextId() { return nextId++; }

    public Project save(Project project) {
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getProjectId() == project.getProjectId()) {
                projects.set(i, project);
                return project;
            }
        }
        projects.add(project);
        return project;
    }

    public Project findById(int id) {
        for (Project p : projects) {
            if (p.getProjectId() == id) return p;
        }
        return null;
    }

    public Project findByName(String name) {
        for (Project p : projects) {
            if (p.getName().equalsIgnoreCase(name)) return p;
        }
        return null;
    }

    public List<Project> getAll() { return new ArrayList<>(projects); }
}