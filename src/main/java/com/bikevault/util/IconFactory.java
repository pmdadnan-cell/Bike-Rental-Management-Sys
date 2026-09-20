package com.bikevault.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Transparent line icons for the admin Control Center. No letter boxes or white plates.
 */
public final class IconFactory {

    public enum Kind {
        DASHBOARD, APPROVALS, RENTALS, BIKES, CUSTOMERS, PAYMENTS, PENALTIES,
        INCIDENTS, AUDIT, REPORTS, SETTINGS, LOGOUT, REVENUE, AVAILABLE,
        ACTIVE, PENDING, FLEET, MAINTENANCE, SEARCH, BELL
    }

    private static final Color GOLD = Color.web("#F4B72A");
    private static final Color MUTED = Color.web("#9AA6B2");
    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private IconFactory() {
    }

    public static Image nav(Kind kind, boolean active, int size) {
        String key = "nav-" + kind + "-" + active + "-" + size;
        return CACHE.computeIfAbsent(key, ignored -> draw(kind, size, active ? GOLD : MUTED));
    }

    public static Image glyph(Kind kind, String hex, int size) {
        String key = "glyph-" + kind + "-" + hex + "-" + size;
        return CACHE.computeIfAbsent(key, ignored -> draw(kind, size, Color.web(hex)));
    }

    public static Image metric(Kind kind, String accentHex, int size) {
        String key = "metric-" + kind + "-" + accentHex + "-" + size;
        return CACHE.computeIfAbsent(key, ignored -> {
            Color accent = Color.web(accentHex);
            Canvas canvas = new Canvas(size, size);
            GraphicsContext g = canvas.getGraphicsContext2D();
            g.setFill(Color.web(accentHex, 0.14));
            g.fillRoundRect(1, 1, size - 2, size - 2, 12, 12);
            drawSymbol(g, kind, size, accent, size * 0.24);
            return snapshot(canvas);
        });
    }

    private static Image draw(Kind kind, int size, Color stroke) {
        Canvas canvas = new Canvas(size, size);
        drawSymbol(canvas.getGraphicsContext2D(), kind, size, stroke, 1.5);
        return snapshot(canvas);
    }

    private static Image snapshot(Canvas canvas) {
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        return canvas.snapshot(parameters, null);
    }

    private static void drawSymbol(GraphicsContext g, Kind kind, double s, Color color, double pad) {
        g.setStroke(color);
        g.setFill(color);
        g.setLineWidth(Math.max(1.6, s * 0.08));
        g.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        g.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
        double x = pad;
        double y = pad;
        double w = s - pad * 2;
        double h = s - pad * 2;
        switch (kind) {
            case DASHBOARD -> {
                g.strokeRoundRect(x, y, w * 0.40, h * 0.40, 2, 2);
                g.strokeRoundRect(x + w * 0.60, y, w * 0.40, h * 0.40, 2, 2);
                g.strokeRoundRect(x, y + h * 0.60, w * 0.40, h * 0.40, 2, 2);
                g.strokeRoundRect(x + w * 0.60, y + h * 0.60, w * 0.40, h * 0.40, 2, 2);
            }
            case APPROVALS -> {
                g.strokeOval(x + 0.5, y + 0.5, w - 1, h - 1);
                g.strokeLine(x + w * 0.28, y + h * 0.52, x + w * 0.44, y + h * 0.68);
                g.strokeLine(x + w * 0.44, y + h * 0.68, x + w * 0.74, y + h * 0.32);
            }
            case RENTALS -> {
                g.strokeRoundRect(x + w * 0.10, y + h * 0.20, w * 0.80, h * 0.70, 3, 3);
                g.strokeLine(x + w * 0.10, y + h * 0.42, x + w * 0.90, y + h * 0.42);
                g.strokeLine(x + w * 0.30, y + h * 0.04, x + w * 0.30, y + h * 0.28);
                g.strokeLine(x + w * 0.70, y + h * 0.04, x + w * 0.70, y + h * 0.28);
            }
            case BIKES -> {
                g.strokeOval(x, y + h * 0.46, w * 0.32, h * 0.42);
                g.strokeOval(x + w * 0.68, y + h * 0.46, w * 0.32, h * 0.42);
                g.strokeLine(x + w * 0.28, y + h * 0.62, x + w * 0.46, y + h * 0.26);
                g.strokeLine(x + w * 0.46, y + h * 0.26, x + w * 0.78, y + h * 0.62);
                g.strokeLine(x + w * 0.46, y + h * 0.26, x + w * 0.38, y + h * 0.10);
            }
            case CUSTOMERS -> {
                g.strokeOval(x + w * 0.32, y + 0.5, w * 0.36, h * 0.36);
                g.strokeArc(x + w * 0.10, y + h * 0.50, w * 0.80, h * 0.62, 12, 156, javafx.scene.shape.ArcType.OPEN);
            }
            case PAYMENTS, REVENUE -> {
                g.strokeRoundRect(x, y + h * 0.22, w, h * 0.56, 5, 5);
                g.strokeLine(x + w * 0.16, y + h * 0.40, x + w * 0.50, y + h * 0.40);
                g.strokeLine(x + w * 0.16, y + h * 0.58, x + w * 0.36, y + h * 0.58);
            }
            case PENALTIES -> {
                g.strokePolygon(new double[]{x + w / 2, x + w - 0.5, x + 0.5}, new double[]{y + 0.5, y + h - 0.5, y + h - 0.5}, 3);
                g.strokeLine(x + w / 2, y + h * 0.38, x + w / 2, y + h * 0.62);
                g.strokeOval(x + w / 2 - 1.1, y + h * 0.72, 2.2, 2.2);
            }
            case INCIDENTS -> {
                g.strokePolygon(
                        new double[]{x + w / 2, x + w * 0.86, x + w * 0.72, x + w * 0.28, x + w * 0.14},
                        new double[]{y + 0.5, y + h * 0.28, y + h - 0.5, y + h - 0.5, y + h * 0.28}, 5);
                g.strokeLine(x + w / 2, y + h * 0.36, x + w / 2, y + h * 0.58);
                g.strokeOval(x + w / 2 - 1.1, y + h * 0.68, 2.2, 2.2);
            }
            case AUDIT -> {
                g.strokeRoundRect(x + w * 0.12, y, w * 0.76, h, 3, 3);
                g.strokeLine(x + w * 0.28, y + h * 0.30, x + w * 0.72, y + h * 0.30);
                g.strokeLine(x + w * 0.28, y + h * 0.50, x + w * 0.72, y + h * 0.50);
                g.strokeLine(x + w * 0.28, y + h * 0.70, x + w * 0.58, y + h * 0.70);
            }
            case REPORTS -> {
                g.strokeLine(x, y + h, x + w, y + h);
                g.strokeLine(x, y, x, y + h);
                g.strokeLine(x + w * 0.24, y + h, x + w * 0.24, y + h * 0.48);
                g.strokeLine(x + w * 0.50, y + h, x + w * 0.50, y + h * 0.22);
                g.strokeLine(x + w * 0.76, y + h, x + w * 0.76, y + h * 0.58);
            }
            case SETTINGS -> {
                g.strokeOval(x + w * 0.30, y + h * 0.30, w * 0.40, h * 0.40);
                for (int i = 0; i < 6; i++) {
                    double a = Math.toRadians(i * 60.0);
                    g.strokeLine(
                            x + w / 2 + Math.cos(a) * w * 0.26,
                            y + h / 2 + Math.sin(a) * h * 0.26,
                            x + w / 2 + Math.cos(a) * w * 0.46,
                            y + h / 2 + Math.sin(a) * h * 0.46);
                }
            }
            case LOGOUT -> {
                g.strokeRoundRect(x, y + h * 0.14, w * 0.52, h * 0.72, 3, 3);
                g.strokeLine(x + w * 0.40, y + h / 2, x + w, y + h / 2);
                g.strokeLine(x + w * 0.76, y + h * 0.32, x + w, y + h / 2);
                g.strokeLine(x + w * 0.76, y + h * 0.68, x + w, y + h / 2);
            }
            case AVAILABLE, FLEET -> {
                g.strokeOval(x + w * 0.08, y + h * 0.22, w * 0.84, h * 0.56);
                g.strokeLine(x + w * 0.22, y + h * 0.50, x + w * 0.78, y + h * 0.50);
            }
            case ACTIVE -> {
                g.strokeOval(x + w * 0.16, y + h * 0.16, w * 0.68, h * 0.68);
                g.fillPolygon(
                        new double[]{x + w * 0.40, x + w * 0.70, x + w * 0.40},
                        new double[]{y + h * 0.34, y + h * 0.50, y + h * 0.66}, 3);
            }
            case PENDING -> {
                g.strokeOval(x + w * 0.10, y + h * 0.10, w * 0.80, h * 0.80);
                g.strokeLine(x + w / 2, y + h * 0.28, x + w / 2, y + h * 0.52);
                g.strokeLine(x + w / 2, y + h * 0.52, x + w * 0.68, y + h * 0.64);
            }
            case MAINTENANCE -> {
                g.strokeLine(x + w * 0.18, y + h * 0.82, x + w * 0.82, y + h * 0.18);
                g.strokeOval(x + w * 0.06, y + h * 0.58, w * 0.28, h * 0.28);
                g.strokeOval(x + w * 0.66, y + h * 0.08, w * 0.28, h * 0.28);
            }
            case SEARCH -> {
                g.strokeOval(x, y, w * 0.62, h * 0.62);
                g.strokeLine(x + w * 0.54, y + h * 0.54, x + w * 0.96, y + h * 0.96);
            }
            case BELL -> {
                g.strokeArc(x + w * 0.18, y + h * 0.10, w * 0.64, h * 0.62, 200, 140, javafx.scene.shape.ArcType.OPEN);
                g.strokeLine(x + w * 0.18, y + h * 0.48, x + w * 0.18, y + h * 0.70);
                g.strokeLine(x + w * 0.82, y + h * 0.48, x + w * 0.82, y + h * 0.70);
                g.strokeLine(x + w * 0.18, y + h * 0.70, x + w * 0.82, y + h * 0.70);
                g.strokeOval(x + w * 0.42, y + h * 0.76, w * 0.16, h * 0.14);
            }
        }
    }
}
