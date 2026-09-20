package com.bikevault.util;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Lightweight motion used for workspace swaps. Kept short so navigation stays snappy.
 */
public final class UiMotion {

    private UiMotion() {
    }

    public static void fadeIn(Node node) {
        if (node == null) {
            return;
        }
        FadeTransition fade = new FadeTransition(Duration.millis(140), node);
        fade.setFromValue(0.35);
        fade.setToValue(1);
        fade.play();
    }
}
