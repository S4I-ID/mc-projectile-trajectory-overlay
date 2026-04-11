package s4i.pto.utils;

import s4i.pto.config.ModConfig;
import s4i.pto.model.Color4f;
import s4i.pto.model.LineSource;

public class ColorUtils {
    public static final Color4f INVISIBLE = new Color4f(0, 0, 0, 0);

    public static Color4f getLineColor(ModConfig config, float charge, LineSource lineSource) {
        switch (lineSource) {
            case SELF -> {
                Color4f baseLineColor = Color4f.of(config.lineColor, config.lineColorA);
                Color4f maxDrawColor = Color4f.of(config.maxDrawColor, config.maxDrawColorA);
                if (config.showTrajectoryColorGradientDependingOnPower) {
                    return mix(baseLineColor, maxDrawColor, charge);
                }
                if (config.showDifferentTrajectoryColorAtMaxPower && charge == 1.0f) {
                    return maxDrawColor;
                }
                return baseLineColor;
            }
            case PLAYER -> {
                return Color4f.of(config.alternativePlayerLineColor, config.alternativePlayerLineColorA);
            }
            case PROJECTILE -> {
                return Color4f.of(config.alternativeProjectileLineColor, config.alternativeProjectileLineColorA);
            }
            case null, default -> {
                return new Color4f();
            }
        }
    }

    public static Color4f getHighlightPointColor(ModConfig config, LineSource lineSource) {
        switch (lineSource) {
            case SELF -> {
                return Color4f.of(config.highlightPointColor, 1.0f);
            }
            case PLAYER, PROJECTILE -> {
                return Color4f.of(config.alternativeHighlightPointColor, config.alternativeHighlightPointColorA);
            }
            case null, default -> {
                return new Color4f();
            }
        }
    }

    public static Color4f getHighlightEntityColor(ModConfig config, LineSource lineSource, boolean isHostile) {
        switch (lineSource) {
            case SELF -> {
                if (isHostile) {
                    return Color4f.of(config.highlightHostileEntityColor, config.highlightEntityColorA);
                } else {
                    return Color4f.of(config.highlightEntityColor, config.highlightEntityColorA);
                }
            }
            case PLAYER, PROJECTILE -> {
                return Color4f.of(config.alternativeHighlightEntityColor, config.alternativeHighlightEntityColorA);
            }
            case null, default -> {
                return new Color4f();
            }
        }
    }

    public static Color4f getHighlightEntityOutlineColor(ModConfig config, LineSource lineSource, boolean isHostile) {
        switch (lineSource) {
            case SELF -> {
                if (isHostile) {
                    return Color4f.of(config.highlightHostileEntityColor, 1.0f);
                } else {
                    return Color4f.of(config.highlightEntityColor, 1.0f);
                }
            }
            case PLAYER, PROJECTILE -> {
                return Color4f.of(config.alternativeHighlightEntityColor, 0.9f);
            }
            case null, default -> {
                return new Color4f();
            }
        }
    }

    public static Color4f getHighlightBlockColor(ModConfig config, LineSource lineSource) {
        switch (lineSource) {
            case SELF -> {
                return Color4f.of(config.highlightBlockColor, 1.0f);
            }
            case PLAYER, PROJECTILE -> {
                return Color4f.of(config.alternativeHighlightBlockColor, 1.0f);
            }
            case null, default -> {
                return new Color4f();
            }
        }
    }

    public static Color4f getDeviationColor(ModConfig config, LineSource lineSource) {
        switch (lineSource) {
            case SELF -> {
                return Color4f.of(config.deviationColor, config.deviationColorA);
            }
            case null, default -> {
                return INVISIBLE;
            }
        }
    }

    private static Color4f mix(Color4f c1, Color4f c2, float percentage) {
        return Color4f.of(
                (int) ((1.0f - percentage) * c1.r + percentage * c2.r),
                (int) ((1.0f - percentage) * c1.g + percentage * c2.g),
                (int) ((1.0f - percentage) * c1.b + percentage * c2.b),
                (int) ((1.0f - percentage) * c1.a + percentage * c2.a)
        );
    }
}
