package finalyear.project.cse.refresh;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

/**
 * Common interface for group refreshing.
 *
 */
public interface RefreshInterface {

    void run();

    void hardShutdown();

    boolean isAlive();

    void start();

    LongProperty newVideosProperty();

    LongProperty totalVideosProperty();

    LongProperty newCommentsProperty();

    LongProperty totalCommentsProperty();

    LongProperty newModeratedProperty();

    LongProperty totalModeratedProperty();

    LongProperty newViewersProperty();

    LongProperty totalViewersProperty();

    BooleanProperty endedProperty();

    DoubleProperty progressProperty();

    StringProperty statusStepProperty();

    StringProperty elapsedTimeProperty();

    ObservableList<String> getObservableErrorList();

    Boolean isEndedOnError();

    Boolean isHardShutdown();
}
