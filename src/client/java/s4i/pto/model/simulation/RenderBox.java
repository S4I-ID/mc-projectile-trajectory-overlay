package s4i.pto.model.simulation;

import net.minecraft.util.math.Box;
import s4i.pto.model.Color4f;

import java.util.ArrayList;
import java.util.List;

public class RenderBox {
    private Box box;
    private List<Color4f> fillColors;
    private List<Color4f> outlineColors;

    public RenderBox(Box box, Color4f fillColor, Color4f outlineColor) {
        this.box = box;
        this.fillColors = new ArrayList<>();
        if (fillColor != null) {
            this.fillColors.add(fillColor);
        }

        this.outlineColors = new ArrayList<>();
        if (outlineColor != null) {
            this.outlineColors.add(outlineColor);
        }
    }

    public RenderBox(Box box) {
        this.box = box;
        this.fillColors = new ArrayList<>();
        this.outlineColors = new ArrayList<>();
    }

    public Box getBox() {
        return box;
    }

    public boolean hasFill() {
        return fillColors != null && !fillColors.isEmpty();
    }

    public boolean hasOutline() {
        return outlineColors != null && !outlineColors.isEmpty();
    }

    public Color4f getFillColor() {
        return mergeColors(fillColors);
    }

    public Color4f getOutlineColor() {
        return mergeColors(outlineColors);
    }

    public RenderBox addFillColor(Color4f color) {
        fillColors.add(color);
        return this;
    }

    public RenderBox addOutlineColor(Color4f color) {
        outlineColors.add(color);
        return this;
    }

    private static Color4f mergeColors(List<Color4f> colors) {
        if (colors == null || colors.isEmpty()) {
            return null;
        }
        int r = 0, g = 0, b = 0, a = 0;
        int size = colors.size();
        for (Color4f color : colors) {
            r += color.r;
            g += color.g;
            b += color.b;
            a += color.a;
        }
        return Color4f.of(r / size, g / size, b / size, a / size);
    }

    @Override
    public String toString() {
        return "BoxDto[box=%s, fillColor=%s, outlineColor=%s]".formatted(box, fillColors, outlineColors);
    }
}
