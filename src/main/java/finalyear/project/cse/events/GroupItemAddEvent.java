package finalyear.project.cse.events;

import finalyear.project.cse.database.Group;

public class GroupItemAddEvent {

    private Group group;

    public GroupItemAddEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
