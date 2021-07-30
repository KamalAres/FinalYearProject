package finalyear.project.cse.events;

import finalyear.project.cse.database.Group;

public class GroupItemDeleteEvent {

    private Group group;

    public GroupItemDeleteEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
