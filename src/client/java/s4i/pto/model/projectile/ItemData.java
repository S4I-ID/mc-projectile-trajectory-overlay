package s4i.pto.model.projectile;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class ItemData {
    public Class<?> clas;
    public ItemStack stack;
    public Item item;

    private ItemData(Class<?> clas, ItemStack stack) {
        this.clas = clas;
        this.stack = stack;
        this.item = stack.getItem();
    }

    public static ItemData of(Class<?> clas, ItemStack stack) {
        return new ItemData(clas, stack);
    }
}
