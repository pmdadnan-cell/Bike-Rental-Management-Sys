package com.bikevault.app;

/**
 * Optional lifecycle hooks for workspace modules loaded into the dashboard center.
 */
public interface WorkspaceView {

    default void onLeave() {
        // Camera, timers, and other resources stop here.
    }

    default void addRecord() {
    }

    default void saveRecord() {
    }

    default void clearForm() {
    }
}
