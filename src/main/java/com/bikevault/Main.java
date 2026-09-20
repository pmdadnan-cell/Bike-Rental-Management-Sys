package com.bikevault;

import com.bikevault.database.SchemaMigrator;
import com.bikevault.util.NavigationUtil;
import javafx.application.Application;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application entry point for BIKEVAULT.
 */
public class Main extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Override
    public void start(Stage primaryStage) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) ->
                LOG.error("Unhandled error on thread {}", thread.getName(), throwable));

        SchemaMigrator.ensure();
        NavigationUtil.initialize(primaryStage);
        NavigationUtil.showLogin();
        LOG.info("BIKEVAULT desktop login window opened. This is not a web server.");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
