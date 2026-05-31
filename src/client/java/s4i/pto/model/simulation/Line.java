package s4i.pto.model.simulation;

import lombok.Getter;
import net.minecraft.world.phys.Vec3;
import s4i.pto.model.Color4f;
import s4i.pto.model.LineSource;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Line {
    private final List<LineSegment> segments;
    private final int deviationId;
    private final LineSource lineSource;

    public Line(int deviationId, LineSource lineSource) {
        this.segments = new ArrayList<>();
        this.deviationId = deviationId;
        this.lineSource = lineSource;
    }

    public void addVertex(Vec3 startPos, Vec3 endPos, Color4f color) {
        this.segments.add(LineSegment.of(startPos, endPos, color));
    }

    public void removeFirstVertex() {
        if (this.segments.size() > 1) {
            this.segments.removeFirst();
        }
    }
}
