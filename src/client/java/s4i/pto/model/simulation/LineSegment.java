package s4i.pto.model.simulation;


import net.minecraft.util.math.Vec3d;
import s4i.pto.model.Color4f;

public class LineSegment {
    public Vec3d start;
    public Vec3d end;
    public Color4f color;

    public LineSegment(Vec3d start, Vec3d end, Color4f color) {
        this.start = start;
        this.end = end;
        this.color = color;
    }

    public static LineSegment of(Vec3d start, Vec3d end, Color4f color) {
        return new LineSegment(start, end, color);
    }

    @Override
    public String toString() {
        return String.format("(%s, %s, %s):(%s, %s, %s) - color %s", start.x, start.y, start.z, end.x, end.y, end.z, color);
    }
}
