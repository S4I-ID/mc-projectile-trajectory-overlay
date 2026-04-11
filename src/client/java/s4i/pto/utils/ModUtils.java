package s4i.pto.utils;

import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

public class ModUtils {
    /**
     * Checks the Fabric API if certain mod is loaded
     * @deprecated will implement if mod support is asked for
     * @return true/false
     */
    @Deprecated
    public static boolean isModLoaded() {
        throw new NotImplementedException();
    }

    public static <T, V> boolean isClassOrSuperClass(T toCheck, Class<V> clas) {
        return clas.isAssignableFrom(toCheck.getClass());
    }

    public static <T> boolean isInClassesOrSuperClasses(T toCheck, List<Class<?>> list) {
        for (Class<?> clas : list) {
            if (clas.isAssignableFrom(toCheck.getClass())) {
                return true;
            }
        }
        return false;
    }
}
