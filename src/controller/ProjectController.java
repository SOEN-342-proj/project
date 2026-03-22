package controller;

import model.Collaborator;
import model.Project;
import repository.CollaboratorRepository;
import repository.ProjectRepository;

/** Handles project-related business logic. */
public class ProjectController {
    private final ProjectRepository projectRepo;
    private final CollaboratorRepository collaboratorRepo;

    public ProjectController(ProjectRepository projectRepo, CollaboratorRepository collaboratorRepo) {
        this.projectRepo = projectRepo;
        this.collaboratorRepo = collaboratorRepo;
    }

    public int createProject(String name, String description) {
        if (projectRepo.findByName(name) != null) {
            System.out.println("Project '" + name + "' already exists.");
            return -1;
        }
        int id = projectRepo.nextId();
        Project project = new Project(id, name, description);
        projectRepo.save(project);
        return id;
    }

    public void updateProject(int projectId, String name, String description) {
        Project project = projectRepo.findById(projectId);
        if (project == null) { System.out.println("Project not found: " + projectId); return; }
        if (name != null && !name.trim().isEmpty()) project.setName(name);
        if (description != null) project.setDescription(description);
    }

    public int addCollaboratorToProject(int projectId, String name, model.enums.Category category) {
        Project project = projectRepo.findById(projectId);
        if (project == null) { System.out.println("Project not found: " + projectId); return -1; }
        Collaborator collaborator = collaboratorRepo.findByName(name);
        if (collaborator == null) {
            int cid = collaboratorRepo.nextId();
            collaborator = new Collaborator(cid, name, category);
            collaboratorRepo.save(collaborator);
        }
        project.addCollaborator(collaborator);
        return collaborator.getCollaboratorId();
    }

    public Project getProject(int projectId) { return projectRepo.findById(projectId); }

    public Project getProjectByName(String name) { return projectRepo.findByName(name); }

    public ProjectRepository getProjectRepo() { return projectRepo; }
}