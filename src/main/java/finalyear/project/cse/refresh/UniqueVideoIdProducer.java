package finalyear.project.cse.refresh;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import finalyear.project.cse.comments.CommentSuite;
import finalyear.project.cse.database.CommentDatabase;
import finalyear.project.cse.util.ExecutorGroup;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class UniqueVideoIdProducer extends ConsumerMultiProducer<String> {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorGroup executorGroup = new ExecutorGroup(1);

    private final CommentDatabase database;

    private long totalVideos;
    private long newVideos;

    public UniqueVideoIdProducer() {
        this.database = CommentSuite.getDatabase();
    }

    @Override
    public void startProducing() {
        executorGroup.submitAndShutdown(this::produce);
    }

    private void produce() {
        logger.debug("Starting UniqueVideoIdProducer");

        final BlockingQueue<String> queue = getBlockingQueue();

        final Set<String> uniqueVideoIds = new HashSet<>(queue);
        totalVideos = uniqueVideoIds.size();
        newVideos = database.countVideosNotExisting(uniqueVideoIds);

        sendCollection(uniqueVideoIds, String.class);

        addProcessed(queue.size());
        queue.clear();

        logger.debug("Ending UniqueVideoIdProducer");
    }

    public long getTotalVideos() {
        return totalVideos;
    }

    public long getNewVideos() {
        return newVideos;
    }

    @Override
    public ExecutorGroup getExecutorGroup() {
        return executorGroup;
    }
}
