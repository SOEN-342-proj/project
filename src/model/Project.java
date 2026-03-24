package model;

import java.util.ArrayList;
import java.util.List;

/** Represents a project. */
public class Project {
    private int projectId;
    private String name;
    private String description;
    private List<Collaborator> collaborators;

    public Project(int projectId, String name, String description) {
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.collaborators = new ArrayList<>();
    }

    public int getProjectId() { return projectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Collaborator> getCollaborators() { return collaborators; }
    public void addCollaborator(Collaborator c) { collaborators.add(c); }

    public Collaborator findCollaboratorByName(String name) {
        for (Collaborator c : collaborators) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    @Override
    public String toString() { return name; }
}