package com.bikevault.util;

import com.bikevault.exception.DatabaseException;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Runs database work off the JavaFX thread and delivers results back on it.
 */
public final class UiAsync {

    private static final Logger LOG = LoggerFactory.getLogger(UiAsync.class);

    private UiAsync() {
    }

    public static <T> void run(Supplier<T> background, Consumer<T> onSuccess) {
        run(background, onSuccess, UiAsync::defaultError);
    }

    public static <T> void run(Supplier<T> background, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return background.get();
            }
        };
        task.setOnSucceeded(event -> onSuccess.accept(task.getValue()));
        task.setOnFailed(event -> {
            Throwable error = task.getException();
            LOG.error("Background task failed", error);
            onError.accept(error);
        });
        Thread thread = new Thread(task, "bikevault-ui-async");
        thread.setDaemon(true);
        thread.start();
    }

    public static void runLater(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    private static void defaultError(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        if (root instanceof DatabaseException || error instanceof DatabaseException) {
            AlertUtil.error("Database error", root.getMessage());
            return;
        }
        if (root instanceof IllegalArgumentException || root instanceof IllegalStateException) {
            AlertUtil.warn("Cannot complete", root.getMessage());
            return;
        }
        AlertUtil.error("Unexpected error", "The operation could not be completed. See the log for details.");
    }
}
