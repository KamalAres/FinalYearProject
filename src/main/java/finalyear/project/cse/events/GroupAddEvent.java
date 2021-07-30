package finalyear.project.cse.events;

import java.util.List;

import finalyear.project.cse.database.Group;

public class GroupAddEvent {

    private List<Group> groups;

    public GroupAddEvent(List<Group> groups) {
        this.groups = groups;
    }

    public List<Group> getGroups() {
        return groups;
    }

}
