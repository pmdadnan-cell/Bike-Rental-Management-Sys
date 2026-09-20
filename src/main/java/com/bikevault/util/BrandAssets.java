package com.bikevault.util;

import javafx.scene.image.Image;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the original BikeVault logo assets. Canvas marks are only a last-resort fallback.
 */
public final class BrandAssets {

    private static final String MARK = "/com/bikevault/images/logo/bikevault-mark.png";
    private static final String WORDMARK = "/com/bikevault/images/logo/bikevault-wordmark.png";
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private BrandAssets() {
    }

    public static Image mark() {
        return load(MARK, () -> BrandGraphics.createMark(128));
    }

    public static Image wordmark() {
        return load(WORDMARK, () -> BrandGraphics.createHorizontal(320, 72));
    }

    private static Image load(String classpath, java.util.function.Supplier<Image> fallback) {
        return CACHE.computeIfAbsent(classpath, key -> {
            try (InputStream stream = BrandAssets.class.getResourceAsStream(key)) {
                if (stream != null) {
                    return new Image(stream, 0, 0, true, true);
                }
            } catch (Exception ignored) {
                // fall through to generated mark
            }
            return fallback.get();
        });
    }
}
