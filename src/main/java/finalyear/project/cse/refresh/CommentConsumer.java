package finalyear.project.cse.refresh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import finalyear.project.cse.comments.CommentSuite;
import finalyear.project.cse.database.CommentDatabase;
import finalyear.project.cse.database.YouTubeComment;
import finalyear.project.cse.util.ElapsedTime;
import finalyear.project.cse.util.ExecutorGroup;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class CommentConsumer extends ConsumerMultiProducer<YouTubeComment> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(10);

    private final boolean moderated;
    private final CommentDatabase database;

    private AtomicLong totalComments = new AtomicLong();
    private AtomicLong newComments = new AtomicLong();

    public CommentConsumer(final boolean moderated) {
        this.moderated = moderated;
        this.database = CommentSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting CommentConsumer " + moderated);

        final ElapsedTime elapsedTime = new ElapsedTime();
        final List<YouTubeComment> comments = new ArrayList<>();
        while (shouldKeepAlive()) {
            final YouTubeComment comment = getBlockingQueue().poll();
            if (comment == null) {
                awaitMillis(100);
                continue;
            } else {
                comments.add(comment);
                addProcessed(1);
            }

            if (comments.size() >= 1000 || (elapsedTime.getElapsed().toMillis() >= 2000 && !comments.isEmpty())) {
                insertComments(comments);
                elapsedTime.setNow();
            }
        }

        if (!comments.isEmpty()) {
            insertComments(comments);
        }

        logger.debug("Ending CommentConsumer " + moderated);
    }

    private void insertComments(final List<YouTubeComment> comments) {
        try {
            if (moderated) {
                logger.debug("Inserting moderated comments {}", comments.size());
            }

            comments.removeIf(comment -> moderated
                    && (comment.getModerationStatus() == null ||
                    comment.getModerationStatus() == ModerationStatus.PUBLISHED));

            final List<String> commentIds = comments.stream()
                .map(YouTubeComment::getId)
                .collect(Collectors.toList());

            totalComments.addAndGet(comments.size());

            if (moderated) {
                newComments.addAndGet(database.countModeratedCommentsNotExisting(commentIds));

                comments.removeIf(comment -> comment.getModerationStatus() == null || comment.getModerationStatus() == ModerationStatus.PUBLISHED);

                database.moderatedComments().insertAll(comments);
                logger.debug("Inserted moderated comments {}", comments.size());
            } else {
                newComments.addAndGet(database.countCommentsNotExisting(commentIds));
                database.comments().insertAll(comments);
                logger.debug("Inserted comments {}", comments.size());
            }

            comments.clear();
        } catch (SQLException e) {
            logger.error("Error on comment submit", e);
        }
    }

    public AtomicLong getTotalComments() {
        return totalComments;
    }

    public AtomicLong getNewComments() {
        return newComments;
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
