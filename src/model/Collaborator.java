package model;
import model.enums.Category;

/** Represents a collaborator on a project. */
public class Collaborator {
    private int collaboratorId;
    private String name;
    private Category category;
    private int openTaskLimit;
    private int openTaskCount;

    public Collaborator(int collaboratorId, String name, Category category) {
        this.collaboratorId = collaboratorId;
        this.name = name;
        this.category = category;
        this.openTaskLimit = category.getTaskLimit();
        this.openTaskCount = 0;
    }

    public int getCollaboratorId() { return collaboratorId; }

    public String getName() { return name; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) {
        this.category = category;
        this.openTaskLimit = category.getTaskLimit();
    }

    public int getOpenTaskLimit() { return openTaskLimit; }
    public void setOpenTaskLimit(int limit) { this.openTaskLimit = limit; }

    public int getOpenTaskCount() { return openTaskCount; }
    public void incrementOpenTaskCount() { openTaskCount++; }
    public void decrementOpenTaskCount() { if (openTaskCount > 0) openTaskCount--; }
    public void setOpenTaskCount(int count) { this.openTaskCount = count; }

    public boolean canAcceptTask() { return openTaskCount < openTaskLimit; }

    @Override
    public String toString() { return name + " (" + category + ")"; }
}