package com.bikevault.util;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/**
 * Generates compact brand, metric, and bike-card images at runtime.
 */
public final class BrandGraphics {

    private static final Color GOLD = Color.web("#D4A017");
    private static final Color GOLD_SOFT = Color.web("#F0C35A");
    private static final Color INK = Color.web("#0B1018");
    private static final Color CARD = Color.web("#1A2233");
    private static final Color MUTED = Color.web("#8B95A8");

    private BrandGraphics() {
    }

    /**
     * Original BIKEVAULT mark: gold monogram over a wheel / motion ring with a QR corner.
     */
    public static Image createMark(int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        double s = size;

        LinearGradient plate = new LinearGradient(0, 0, s, s, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#16120B")),
                new Stop(1, Color.web("#2A2114")));
        g.setFill(plate);
        g.fillRoundRect(0, 0, s, s, s * 0.24, s * 0.24);

        g.setStroke(GOLD);
        g.setLineWidth(Math.max(1.6, s * 0.07));
        g.strokeOval(s * 0.12, s * 0.12, s * 0.76, s * 0.76);
        g.setLineWidth(Math.max(1.0, s * 0.03));
        g.setStroke(GOLD_SOFT);
        g.strokeOval(s * 0.24, s * 0.24, s * 0.52, s * 0.52);

        g.setStroke(GOLD.deriveColor(0, 1, 1, 0.55));
        g.setLineWidth(Math.max(0.8, s * 0.025));
        g.strokeLine(s * 0.50, s * 0.16, s * 0.50, s * 0.84);
        g.strokeLine(s * 0.16, s * 0.50, s * 0.84, s * 0.50);
        g.strokeLine(s * 0.26, s * 0.26, s * 0.74, s * 0.74);

        double q = s * 0.11;
        g.setFill(GOLD);
        g.fillRect(s * 0.08, s * 0.08, q, q);
        g.fillRect(s * 0.08 + q * 0.55, s * 0.08, q * 0.45, q * 0.45);
        g.fillRect(s * 0.81, s * 0.81, q, q);

        g.setFill(GOLD);
        g.setFont(Font.font("Segoe UI", FontWeight.EXTRA_BOLD, s * 0.34));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText("BV", s / 2.0, s * 0.64);
        return canvas.snapshot(null, null);
    }

    public static Image createHorizontal(int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.drawImage(createMark(height), 0, 0);
        g.setFill(GOLD);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, height * 0.38));
        g.setTextAlign(TextAlignment.LEFT);
        g.fillText("BIKEVAULT", height + 10, height * 0.64);
        return canvas.snapshot(null, null);
    }

    public static Image createMetricIcon(String glyph, String fillHex, int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web(fillHex, 0.18));
        g.fillRoundRect(0, 0, size, size, 12, 12);
        g.setStroke(Color.web(fillHex, 0.45));
        g.setLineWidth(1);
        g.strokeRoundRect(0.5, 0.5, size - 1, size - 1, 12, 12);
        g.setFill(Color.web(fillHex));
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, size * 0.38));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(glyph, size / 2.0, size * 0.66);
        return canvas.snapshot(null, null);
    }

    public static Image createSidebarGlyph(String glyph, boolean active, int size) {
        Canvas canvas = new Canvas(size, size);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(active ? GOLD : CARD);
        g.fillRoundRect(0, 0, size, size, 8, 8);
        g.setFill(active ? INK : MUTED);
        g.setFont(Font.font("Segoe UI", FontWeight.SEMI_BOLD, size * 0.42));
        g.setTextAlign(TextAlignment.CENTER);
        g.fillText(glyph, size / 2.0, size * 0.68);
        return canvas.snapshot(null, null);
    }

    public static Image createBikeCard(String brand, String model, int width, int height) {
        Canvas canvas = new Canvas(width, height);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.web("#1C2434"));
        g.fillRoundRect(0, 0, width, height, 16, 16);
        g.setFill(GOLD);
        g.fillRoundRect(16, height * 0.38, width - 32, height * 0.18, 10, 10);
        g.setFill(Color.web("#F3E6C0"));
        g.fillOval(width * 0.18, height * 0.52, 28, 28);
        g.fillOval(width * 0.68, height * 0.52, 28, 28);
        g.setFill(GOLD);
        g.setFont(Font.font("Segoe UI", FontWeight.BOLD, 16));
        g.setTextAlign(TextAlignment.CENTER);
        String label = ((brand == null ? "" : brand) + " " + (model == null ? "" : model)).trim();
        if (label.isBlank()) {
            label = "BIKE";
        }
        g.fillText(label, width / 2.0, 28);
        return canvas.snapshot(null, null);
    }
}
