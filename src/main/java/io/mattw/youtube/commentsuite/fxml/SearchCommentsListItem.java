package io.mattw.youtube.commentsuite.fxml;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import finalyear.project.cse.comments.CommentSuite;
import finalyear.project.cse.comments.ConfigData;
import finalyear.project.cse.comments.ImageCache;
import finalyear.project.cse.comments.ImageLoader;
import finalyear.project.cse.database.YouTubeChannel;
import finalyear.project.cse.database.YouTubeComment;
import finalyear.project.cse.events.*;
import finalyear.project.cse.refresh.ModerationStatus;
import finalyear.project.cse.util.BrowserUtil;
import finalyear.project.cse.util.DateUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Border;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.time.LocalDateTime;

import static finalyear.project.cse.refresh.ModerationStatus.PUBLISHED;
import static javafx.application.Platform.runLater;

public class SearchCommentsListItem extends HBox implements Cleanable {

    private static final Logger logger = LogManager.getLogger();
    private static final LocalDateTime DAYS_AGO_60 = LocalDateTime.now().minusDays(60);

    @FXML private ImageView thumbnail;
    @FXML private Hyperlink author;
    @FXML private Label commentText;
    @FXML private Label date;
    @FXML private Label type;
    @FXML private Label likes;
    @FXML private Hyperlink showMore, viewTree, reply;
    @FXML private HBox systemTagsPane, userTagsPane;

    private final BrowserUtil browserUtil = new BrowserUtil();
    private final ConfigData configData;
    private final EventBus eventBus;

    private final YouTubeComment comment;
    private final YouTubeChannel channel;
    private boolean showReplyBtn = true;

    public SearchCommentsListItem(final YouTubeComment comment) throws IOException {
        this.comment = comment;
        this.channel = comment.getChannel();

        configData = CommentSuite.getConfig().getDataObject();
        eventBus = CommentSuite.getEventBus();
        eventBus.register(this);

        FXMLLoader loader = new FXMLLoader(getClass().getResource("SearchCommentsListItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        loader.load();

        thumbnail.setImage(channel.getDefaultThumb());
        checkProfileThumb();

        author.setText(channel.getTitle());
        author.setOnAction(ae -> browserUtil.open(channel.buildYouTubeLink()));
        author.setBorder(Border.EMPTY);

        commentText.setText(comment.getCleanText(false).replace("\r\n", " "));
        commentText.setTextOverrun(OverrunStyle.ELLIPSIS);

        date.setText(DateUtils.epochMillisToDateTime(comment.getPublished()).toString());

        if (comment.getReplyCount() > 0 || comment.isReply()) {
            viewTree.setManaged(true);
            viewTree.setVisible(true);
            if (!comment.isReply()) {
                viewTree.setText(String.format("View Thread (%,d)", comment.getReplyCount()));
            }
        }

        final ModerationStatus status = comment.getModerationStatus();
        if (status != null && status != PUBLISHED) {
            this.getStyleClass().add(status.getApiValue());
        }

        if (comment.isReply()) {
            this.getStyleClass().add("reply");
            type.setText("Reply");
        }

        if (comment.getLikes() > 0) {
            likes.setText(String.format("+%,d", comment.getLikes()));
        } else {
            likes.setVisible(false);
            likes.setManaged(false);
        }

        if (status != null && status != PUBLISHED) {
            addTag(systemTagsPane, status.getApiValue());
        }

        if (status != null && status != PUBLISHED && comment.getPublishedDateTime().isBefore(DAYS_AGO_60)) {
            reply.setManaged(false);
            reply.setVisible(false);
            viewTree.setManaged(false);
            viewTree.setVisible(false);
            showReplyBtn = false;
            addTag(systemTagsPane, "past-60-days");
        }

        reloadUserTags();
        determineHideReply();

        showMore.setOnAction(ae -> eventBus.post(new ShowMoreEvent(this)));
        reply.setOnAction(ae -> eventBus.post(new ReplyEvent(this)));
        viewTree.setOnAction(ae -> eventBus.post(new ViewTreeEvent(this)));
    }

    public YouTubeComment getComment() {
        return comment;
    }

    public void addTag(Pane pane, String text) {
        final Label tag = new Label(text);
        tag.getStyleClass().addAll("textMuted", "tag");
        runLater(() -> pane.getChildren().add(tag));
    }

    private void determineHideReply() {
        final boolean display = !configData.getAccounts().isEmpty() && showReplyBtn;

        runLater(() -> {
            reply.setManaged(display);
            reply.setVisible(display);
        });
    }

    @Subscribe
    public void accountAddEvent(final AccountAddEvent accountAddEvent) {
        determineHideReply();
    }

    @Subscribe
    public void accountDeleteEvent(final AccountDeleteEvent accountDeleteEvent) {
        determineHideReply();
    }

    @Subscribe
    public void tagsChangeEvent(final TagsChangeEvent tagsChangeEvent) {
        if (tagsChangeEvent.wasChanged(comment)) {
            tagsChangeEvent.updateTags(comment);

            reloadUserTags();
        }
    }

    public void reloadUserTags() {
        runLater(() -> userTagsPane.getChildren().clear());

        if (comment.getTags() != null) {
            comment.getTags().stream().sorted().forEach(tag -> runLater(() -> addTag(userTagsPane, tag)));
        }
    }

    public void treeMode() {
        viewTree.setVisible(false);
        viewTree.setManaged(false);
    }

    /**
     * Loads profile thumbnail.
     */
    public void loadProfileThumb() {
        runLater(() -> thumbnail.setImage(ImageLoader.LOADING.getImage()));
        Image thumbImage = ImageCache.findOrGetImage(channel);
        runLater(() -> thumbnail.setImage(thumbImage));
    }

    /**
     * Checks if profile thumb loaded and loads if present.
     */
    public void checkProfileThumb() {
        if (ImageCache.hasImageCached(channel)) {
            loadProfileThumb();
        }
    }

    @Override
    public void cleanUp() {
        showMore.setOnAction(null);
        viewTree.setOnAction(null);
        reply.setOnAction(null);
    }
}
