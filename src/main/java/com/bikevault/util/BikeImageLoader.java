package com.bikevault.util;

import com.bikevault.model.Bike;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

import java.io.InputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolves motorcycle images from {@code bikes.image_path} and classpath assets.
 * Controllers never hard-code a model-to-file map; they call this loader.
 */
public final class BikeImageLoader {

    public static final String DEFAULT_PATH = "/images/bikes/mt15.png";
    private static final String CLASSPATH_BIKES = "/com/bikevault/images/bikes/";
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private BikeImageLoader() {
    }

    public static String pathFor(String brand, String model) {
        return "/images/bikes/" + fileNameFor(brand, model);
    }

    public static String pathFor(Bike bike) {
        if (bike != null && bike.getImagePath() != null && !bike.getImagePath().isBlank()) {
            return bike.getImagePath().trim();
        }
        return pathFor(bike == null ? null : bike.getBrand(), bike == null ? null : bike.getModel());
    }

    public static Image imageFor(Bike bike) {
        return imageForPath(pathFor(bike));
    }

    public static Image imageForPath(String imagePath) {
        String classpath = toClasspath(imagePath);
        Image image = CACHE.computeIfAbsent(classpath, BikeImageLoader::read);
        if (image == null || image.isError()) {
            return CACHE.computeIfAbsent(CLASSPATH_BIKES + "mt15.png", BikeImageLoader::read);
        }
        return image;
    }

    public static ImageView viewFor(Bike bike, double width, double height) {
        ImageView view = new ImageView(imageFor(bike));
        view.setFitWidth(width);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    public static StackPane framed(Bike bike, double width, double height) {
        ImageView view = viewFor(bike, width, height);
        StackPane frame = new StackPane(view);
        frame.setPrefSize(width, height);
        frame.setMaxSize(width, height);
        frame.setMinSize(width, height);
        frame.getStyleClass().add("bike-image-frame");
        Rectangle clip = new Rectangle(width, height);
        clip.setArcWidth(18);
        clip.setArcHeight(18);
        frame.setClip(clip);
        return frame;
    }

    public static String fileNameFor(String brand, String model) {
        String text = ((brand == null ? "" : brand) + " " + (model == null ? "" : model))
                .toLowerCase(Locale.ROOT);
        if (contains(text, "classic")) {
            return "classic350.png";
        }
        if (contains(text, "hunter")) {
            return "hunter350.png";
        }
        if (contains(text, "himalayan") || contains(text, "xpulse")) {
            return "himalayan.png";
        }
        if (contains(text, "meteor")) {
            return "meteor350.png";
        }
        if (contains(text, "mt-15") || contains(text, "mt15") || contains(text, "hornet") || contains(text, " fz")) {
            return "mt15.png";
        }
        if (contains(text, "r15") || contains(text, "rc 200") || contains(text, "rc200")) {
            return "r15.png";
        }
        if (contains(text, "duke 390") || contains(text, "duke390")) {
            return "duke390.png";
        }
        if (contains(text, "duke")) {
            return "duke200.png";
        }
        if (contains(text, "apache") || contains(text, "gixxer")) {
            return "apache.png";
        }
        if (contains(text, "450x") || contains(text, "ather") || contains(text, "iqube")) {
            return "ather450x.png";
        }
        if (contains(text, "activa")) {
            return "activa.png";
        }
        if (contains(text, "access")) {
            return "access.png";
        }
        if (contains(text, "aerox") || contains(text, "ntorq")) {
            return "aerox.png";
        }
        if (contains(text, "s1") || contains(text, "ola")) {
            return "ola.png";
        }
        if (contains(text, "ninja")) {
            return "ninja300.png";
        }
        if (contains(text, "cb350")) {
            return "cb350.png";
        }
        if (contains(text, "310 gs") || contains(text, "310gs")) {
            return "g310gs.png";
        }
        if (contains(text, "z650") || contains(text, "dominar") || contains(text, "speed")
                || contains(text, "310 r") || contains(text, "scrambler")) {
            return "z650.png";
        }
        if (contains(text, "pulsar") || contains(text, "splendor") || contains(text, "raider")
                || contains(text, "sp 125") || contains(text, "sp125")) {
            return "pulsar.png";
        }
        return "mt15.png";
    }

    private static boolean contains(String text, String needle) {
        return text.contains(needle);
    }

    private static String toClasspath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return CLASSPATH_BIKES + "mt15.png";
        }
        String trimmed = imagePath.trim().replace('\\', '/');
        if (trimmed.startsWith("/com/bikevault/")) {
            return trimmed;
        }
        int bikes = trimmed.indexOf("/bikes/");
        if (bikes >= 0) {
            return CLASSPATH_BIKES + trimmed.substring(bikes + "/bikes/".length());
        }
        int slash = trimmed.lastIndexOf('/');
        return CLASSPATH_BIKES + (slash >= 0 ? trimmed.substring(slash + 1) : trimmed);
    }

    private static Image read(String classpath) {
        try (InputStream stream = BikeImageLoader.class.getResourceAsStream(classpath)) {
            if (stream == null) {
                return null;
            }
            return new Image(stream, 0, 0, true, true);
        } catch (Exception ex) {
            return null;
        }
    }
}
