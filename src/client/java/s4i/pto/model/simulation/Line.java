package s4i.pto.model.simulation;

import net.minecraft.util.math.Vec3d;
import s4i.pto.model.Color4f;
import s4i.pto.model.LineSource;

import java.util.ArrayList;
import java.util.List;

public class Line {
    private List<LineSegment> segments;
    private int deviationId;
    private LineSource lineSource;

    public Line(int deviationId, LineSource lineSource) {
        this.segments = new ArrayList<>();
        this.deviationId = deviationId;
        this.lineSource = lineSource;
    }

    public void addVertex(Vec3d startPos, Vec3d endPos, Color4f color) {
        this.segments.add(LineSegment.of(startPos, endPos, color));
    }

    public void removeFirstVertex() {
        if (this.segments.size() > 1) {
            this.segments.removeFirst();
        }
    }

    public List<LineSegment> getSegments() {
        return this.segments;
    }

    public int getDeviationId() {
        return this.deviationId;
    }

    public LineSource getLineSource() {
        return this.lineSource;
    }
}
