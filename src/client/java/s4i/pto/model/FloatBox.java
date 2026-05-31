package s4i.pto.model;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import s4i.pto.model.simulation.DeviationBox;

public class FloatBox {
    public final float minX;
    public final float minY;
    public final float minZ;
    public final float maxX;
    public final float maxY;
    public final float maxZ;

    public FloatBox(AABB box) {
        this.minX = (float) box.minX;
        this.minY = (float) box.minY;
        this.minZ = (float) box.minZ;
        this.maxX = (float) box.maxX;
        this.maxY = (float) box.maxY;
        this.maxZ = (float) box.maxZ;
    }

    public FloatBox(AABB box, Vec3 cameraPos) {
        AABB offsetBox = box.move(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        this.minX = (float) offsetBox.minX;
        this.minY = (float) offsetBox.minY;
        this.minZ = (float) offsetBox.minZ;
        this.maxX = (float) offsetBox.maxX;
        this.maxY = (float) offsetBox.maxY;
        this.maxZ = (float) offsetBox.maxZ;
    }

    public FloatBox(DeviationBox box, Vec3 cameraPos) {
        this.minX = (float) (box.minX - cameraPos.x);
        this.minY = (float) (box.minY - cameraPos.y);
        this.minZ = (float) (box.minZ - cameraPos.z);
        this.maxX = (float) (box.maxX - cameraPos.x);
        this.maxY = (float) (box.maxY - cameraPos.y);
        this.maxZ = (float)( box.maxZ - cameraPos.z);
    }
}
