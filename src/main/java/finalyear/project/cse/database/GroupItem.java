package finalyear.project.cse.database;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;

import finalyear.project.cse.comments.CommentSuite;
import finalyear.project.cse.comments.ConfigData;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database entry for searched YouTube entries (Video, Channel, Playlist).
 * getYouTubeId() from YouTubeObject represents the GroupItem ID.
 */
public class GroupItem extends YouTubeObject {

    private static final Logger logger = LogManager.getLogger();

    public static String ALL_ITEMS = "ALL_ITEMS";

    private String channelTitle;
    private long published;
    private long lastChecked;

    private YouTube youtube;
    private CommentDatabase database;
    private boolean fastAdd = false;

    /**
     * Used for converting selected search items for inserting into database.
     */
    public GroupItem(final SearchResult item) {
        super(item.getId(), item.getSnippet().getTitle(),
                item.getSnippet().getThumbnails().getMedium().getUrl());

        this.published = item.getSnippet().getPublishedAt().getValue();
        this.channelTitle = item.getSnippet().getChannelTitle();
        this.lastChecked = 0;

        if (item.getId().getVideoId() != null) setTypeId(YType.VIDEO);
        if (item.getId().getChannelId() != null) setTypeId(YType.CHANNEL);
        if (item.getId().getPlaylistId() != null) setTypeId(YType.PLAYLIST);
    }

    /**
     * Used by ManageGroupsManager when user submits link directly.
     *
     * @param item VideosList.Item
     */
    public GroupItem(final Video item) {
        this(item.getId(), YType.VIDEO, item.getSnippet().getTitle(), item.getSnippet().getChannelTitle(),
                item.getSnippet().getThumbnails().getMedium().getUrl(), item.getSnippet().getPublishedAt().getValue(), 0);
    }

    /**
     * Used by ManageGroupsManager when user submits link directly.
     *
     * @param item ChannelsList.Item
     */
    public GroupItem(final Channel item) {
        this(item.getId(), YType.CHANNEL, item.getSnippet().getTitle(), item.getSnippet().getTitle(),
                item.getSnippet().getThumbnails().getMedium().getUrl(), item.getSnippet().getPublishedAt().getValue(), 0);
    }

    /**
     * Used by ManageGroupsManager when user submits link directly.
     *
     * @param item PlaylistItemsList.Item
     */
    public GroupItem(final Playlist item) {
        this(item.getId(), YType.PLAYLIST, item.getSnippet().getTitle(), item.getSnippet().getChannelTitle(),
                item.getSnippet().getThumbnails().getMedium().getUrl(), item.getSnippet().getPublishedAt().getValue(), 0);
    }

    /**
     * Used for "All Items (#)" and "No items" display in the "Comment Search" and "Group Manager" ComboBoxes.
     */
    public GroupItem(final String gitemId, final String title) {
        super(gitemId, title, null);
    }

    /**
     * Used by the database when querying for group items.
     */
    public GroupItem(final String gitemId, final YType typeId, final String title, final String channelTitle, final String thumbUrl, final long published, final long lastChecked) {
        super(gitemId, title, thumbUrl);
        setTypeId(typeId);
        this.channelTitle = channelTitle;
        this.published = published;
        this.lastChecked = lastChecked;
    }

    /**
     * Parses video, playlist, and channel links to create a GroupItem.
     *
     * @param link a video, playlist, or channel link
     * @throws IOException there was a problem parsing the link
     */
    public GroupItem(final String link) throws IOException {
        ofLink(link);
    }

    public GroupItem(final String link, final boolean fastAdd) throws IOException {
        this.fastAdd = fastAdd;
        ofLink(link);
    }

    private void ofLink(final String fullLink) throws IOException {
        logger.debug("Matching link to type [fullLink={}]", fullLink);

        youtube = CommentSuite.getYouTube();
        database = CommentSuite.getDatabase();

        final Pattern video1 = Pattern.compile("(?:http[s]?://youtu.be/)([\\w_\\-]+)");
        final Pattern video2 = Pattern.compile("(?:http[s]?://www.youtube.com/watch\\?v=)([\\w_\\-]+)");
        final Pattern playlist = Pattern.compile("(?:http[s]?://www.youtube.com/playlist\\?list=)([\\w_\\-]+)");
        final Pattern channel1 = Pattern.compile("(?:http[s]?://www.youtube.com/channel/)([\\w_\\-]+)");
        final Pattern channel2 = Pattern.compile("(?:http[s]?://www.youtube.com/user/)([\\w_\\-]+)");

        Matcher m;
        YType type = YType.UNKNOWN;
        boolean channelUsername = false;
        String result = "";
        if ((m = video1.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.VIDEO;
        } else if ((m = video2.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.VIDEO;
        } else if ((m = playlist.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.PLAYLIST;
        } else if ((m = channel1.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.CHANNEL;
        } else if ((m = channel2.matcher(fullLink)).matches()) {
            result = m.group(1);
            type = YType.CHANNEL;
            channelUsername = true;
        }

        if (fastAdd) {
            if (channelUsername) {
                throw new IOException("Channel usernames are not accepted when using fast group add.");
            }

            setId(result);
            setTitle(result);
            setTypeId(type);
            setChannelTitle(StringUtils.EMPTY);
            setThumbUrl(ConfigData.FAST_GROUP_ADD_THUMB_PLACEHOLDER);
            setPublished(0);
            setLastChecked(0);

            return;
        }


        final YouTube youtube = CommentSuite.getYouTube();
        if (result.isEmpty()) {
            throw new IOException(String.format("Input did not match expected formats [fullLink=%s]", fullLink));
        } else {
            if (type == YType.VIDEO) {
                final VideoListResponse vl = youtube.videos().list("snippet")
                        .setKey(CommentSuite.getYouTubeApiKey())
                        .setId(result)
                        .execute();

                if (!vl.getItems().isEmpty()) {
                    final Video item = vl.getItems().get(0);

                    duplicate(new GroupItem(item));

                    checkForNewChannel(item.getSnippet().getChannelId());
                }
            } else if (type == YType.CHANNEL) {
                YouTube.Channels.List cl = youtube.channels().list("snippet")
                        .setKey(CommentSuite.getYouTubeApiKey());

                if (!channelUsername) {
                    cl = cl.setId(result);
                } else {
                    cl = cl.setForUsername(result);
                }

                final ChannelListResponse clr = cl.execute();

                if (!clr.getItems().isEmpty()) {
                    final Channel item = clr.getItems().get(0);

                    duplicate(new GroupItem(item));

                    checkForNewChannel(item.getId());
                }
            } else {
                final PlaylistListResponse pl = youtube.playlists().list("snippet")
                        .setKey(CommentSuite.getYouTubeApiKey())
                        .setId(result)
                        .execute();

                if (!pl.getItems().isEmpty()) {
                    final Playlist item = pl.getItems().get(0);

                    duplicate(new GroupItem(item));

                    checkForNewChannel(item.getSnippet().getChannelId());
                }
            }
        }
    }

    /**
     * Makes sure the channel associated with this GroupItem is in the database.
     */
    private void checkForNewChannel(final String channelId) {
        try {
            if (database.channels().notExists(channelId)) {
                final ChannelListResponse clr = youtube.channels().list("snippet")
                        .setKey(CommentSuite.getYouTubeApiKey())
                        .setId(channelId)
                        .execute();

                final YouTubeChannel channel = new YouTubeChannel(clr.getItems().get(0));

                database.channels().insert(channel);
            }
        } catch (IOException e) {
            logger.error("Failed to get channel [id={}]", channelId);
        } catch (SQLException e) {
            logger.error("Failed to insert new channel [id={}]", channelId);
        }
    }

    /**
     * Copies fields of submitted GroupItem to this instance's fields.
     *
     * @param groupItem item to copy fields from
     */
    private void duplicate(GroupItem groupItem) {
        setTypeId(groupItem.getTypeId());
        setId(groupItem.getId());
        setTitle(groupItem.getTitle());
        setThumbUrl(groupItem.getThumbUrl());
        setYouTubeLink(groupItem.getYouTubeLink());

        setChannelTitle(groupItem.getChannelTitle());
        setLastChecked(groupItem.getLastChecked());
        setPublished(groupItem.getPublished());
    }

    public String getChannelTitle() {
        return channelTitle;
    }

    public void setChannelTitle(String channelTitle) {
        this.channelTitle = channelTitle;
    }

    public long getPublished() {
        return published;
    }

    public void setPublished(long published) {
        this.published = published;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public void setLastChecked(long timestamp) {
        this.lastChecked = timestamp;
    }

    public String toString() {
        return getTitle();
    }
}
