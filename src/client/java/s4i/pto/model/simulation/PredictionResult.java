package s4i.pto.model.simulation;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PredictionResult {
    public List<Line> lines;
    public Map<Entity, RenderBox> entityMap;
    public Map<BlockPos, RenderBox> blockMap;

    public PredictionResult() {
        this.lines = new ArrayList<>();
        this.entityMap = new HashMap<>();
        this.blockMap = new HashMap<>();
    }

    public void merge(PredictionResult other) {
        this.lines.addAll(other.lines);
        this.entityMap.putAll(other.entityMap);
        this.blockMap.putAll(other.blockMap);
    }
}
