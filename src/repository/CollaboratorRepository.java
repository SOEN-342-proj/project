package repository;

import model.Collaborator;

import java.util.ArrayList;
import java.util.List;

public class CollaboratorRepository {
    private final List<Collaborator> collaborators = new ArrayList<>();
    private int nextId = 1;

    public int nextId() { return nextId++; }

    public Collaborator save(Collaborator c) {
        for (int i = 0; i < collaborators.size(); i++) {
            if (collaborators.get(i).getCollaboratorId() == c.getCollaboratorId()) {
                collaborators.set(i, c);
                return c;
            }
        }
        collaborators.add(c);
        return c;
    }

    public Collaborator findById(int id) {
        for (Collaborator c : collaborators) {
            if (c.getCollaboratorId() == id) return c;
        }
        return null;
    }

    public Collaborator findByName(String name) {
        for (Collaborator c : collaborators) {
            if (c.getName().equalsIgnoreCase(name)) return c;
        }
        return null;
    }

    public List<Collaborator> getAll() { return new ArrayList<>(collaborators); }
}