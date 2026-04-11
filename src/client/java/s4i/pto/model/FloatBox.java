package s4i.pto.model;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import s4i.pto.model.simulation.DeviationBox;

public class FloatBox {
    public final float minX;
    public final float minY;
    public final float minZ;
    public final float maxX;
    public final float maxY;
    public final float maxZ;

    public FloatBox(Box box) {
        this.minX = (float) box.minX;
        this.minY = (float) box.minY;
        this.minZ = (float) box.minZ;
        this.maxX = (float) box.maxX;
        this.maxY = (float) box.maxY;
        this.maxZ = (float) box.maxZ;
    }

    public FloatBox(Box box, Vec3d cameraPos) {
        Box offsetBox = box.offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        this.minX = (float) offsetBox.minX;
        this.minY = (float) offsetBox.minY;
        this.minZ = (float) offsetBox.minZ;
        this.maxX = (float) offsetBox.maxX;
        this.maxY = (float) offsetBox.maxY;
        this.maxZ = (float) offsetBox.maxZ;
    }

    public FloatBox(DeviationBox box, Vec3d cameraPos) {
        this.minX = (float) (box.minX - cameraPos.x);
        this.minY = (float) (box.minY - cameraPos.y);
        this.minZ = (float) (box.minZ - cameraPos.z);
        this.maxX = (float) (box.maxX - cameraPos.x);
        this.maxY = (float) (box.maxY - cameraPos.y);
        this.maxZ = (float)( box.maxZ - cameraPos.z);
    }
}
