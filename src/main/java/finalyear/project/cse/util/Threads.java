package finalyear.project.cse.util;

public class Threads {

    public static void awaitMillis(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

}
