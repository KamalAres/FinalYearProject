package finalyear.project.cse.events;

import java.util.List;

import finalyear.project.cse.database.YouTubeComment;

public class TagsChangeEvent {

    public List<YouTubeComment> comments;

    public TagsChangeEvent(List<YouTubeComment> comments) {
        this.comments = comments;
    }

    public List<YouTubeComment> getComments() {
        return comments;
    }

    public boolean wasChanged(YouTubeComment comment) {
        return comments.stream().anyMatch(changed -> comment.getId().equals(changed.getId()));
    }

    public void updateTags(YouTubeComment comment) {
        comments.stream().filter(changed -> comment.getId().equals(changed.getId()))
                .forEach(changed -> comment.setTags(changed.getTags()));
    }

}
