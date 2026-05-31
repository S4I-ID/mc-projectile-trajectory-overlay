package s4i.pto.model.simulation;


import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PredictionResult {
    private final List<Line> lines;
    private final Map<Entity, RenderBox> entityMap;
    private final Map<BlockPos, RenderBox> blockMap;

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
