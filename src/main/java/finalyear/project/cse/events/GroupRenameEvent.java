package finalyear.project.cse.events;

import finalyear.project.cse.database.Group;

public class GroupRenameEvent {

    private Group group;

    public GroupRenameEvent(Group group) {
        this.group = group;
    }

    public Group getGroup() {
        return group;
    }

}
