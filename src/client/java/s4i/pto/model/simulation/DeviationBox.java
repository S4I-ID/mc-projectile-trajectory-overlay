package s4i.pto.model.simulation;

import net.minecraft.world.phys.Vec3;
import s4i.pto.model.Color4f;

public class DeviationBox {
    public double minX;
    public double minY;
    public double minZ;
    public double maxX;
    public double maxY;
    public double maxZ;

    public int index;
    public Color4f color;

    public DeviationBox(Vec3 corner1, Vec3 corner2, int index, Color4f color) {
        this.minX = Math.min(corner1.x, corner2.x);
        this.minY = Math.min(corner1.y, corner2.y);
        this.minZ = Math.min(corner1.z, corner2.z);
        this.maxX = Math.max(corner1.x, corner2.x);
        this.maxY = Math.max(corner1.y, corner2.y);
        this.maxZ = Math.max(corner1.z, corner2.z);
        this.index = index;
        this.color = color;
    }
}
