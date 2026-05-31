package s4i.pto.model.projectile;

import lombok.Data;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@Data
public class ItemData {
    private Class<?> itemClass;
    private ItemStack stack;
    private Item item;

    private ItemData(Class<?> itemClass, ItemStack stack) {
        this.itemClass = itemClass;
        this.stack = stack;
        this.item = stack.getItem();
    }

    public static ItemData of(Class<?> clas, ItemStack stack) {
        return new ItemData(clas, stack);
    }
}
