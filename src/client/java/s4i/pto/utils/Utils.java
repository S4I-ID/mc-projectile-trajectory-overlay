package s4i.pto.utils;

import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    public static <T> List<T> filterList(List<T> list, Predicate<T> predicate) {
        return Stream.ofNullable(list)
                .flatMap(Collection::stream)
                .filter(predicate)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public static <T> List<T> collectionToList(Collection<T> collection) {
        return new ArrayList<>(collection);
    }

    /**
     * Mix two float values with percentage
     * @param f1 float value 1
     * @param f2 float value 2
     * @param percentage between 0f and 1.0f
     * @return mix float value 2 in given percentage in float value 1
     */
    public static float mixFloats(float f1, float f2, float percentage) {
        return (1.0f - percentage) * f1 + percentage * f2;
    }

    /**
     * Deep copy Vec3d object
     * @param vec3d object to copy
     * @return copied object
     */
    public static Vec3d copyVec3d(Vec3d vec3d) {
        return new Vec3d(vec3d.x, vec3d.y, vec3d.z);
    }

    public static boolean doubleBetween(double fl, double min, double max) {
        return min <= fl && fl <= max;
    }
}
