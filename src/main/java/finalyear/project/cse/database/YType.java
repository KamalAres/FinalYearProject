package finalyear.project.cse.database;

/**
 * Type definition for YouTubeObjects
 *
 */
public enum YType {
    UNKNOWN("Unknown"),
    VIDEO("Video"),
    CHANNEL("Channel"),
    PLAYLIST("Playlist"),
    COMMENT("Comment");

    private String display;

    YType(String display) {
        this.display = display;
    }

    public String getDisplay() {
        return display;
    }

    public int id() {
        return this.ordinal() - 1;
    }
}
