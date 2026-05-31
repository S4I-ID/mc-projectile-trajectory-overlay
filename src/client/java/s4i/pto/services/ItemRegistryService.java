package s4i.pto.services;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.LingeringPotionItem;
import net.minecraft.world.item.SnowballItem;
import net.minecraft.world.item.SplashPotionItem;
import net.minecraft.world.item.ThrowablePotionItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.Vec3;
import s4i.pto.model.projectile.ItemData;
import s4i.pto.model.projectile.ProjectileData;
import s4i.pto.services.initializers.ModdedProjectileItemInitializer;
import s4i.pto.services.initializers.VanillaProjectileItemInitializer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ItemRegistryService {
    private static ItemRegistryService instance;

    /**
     * Each item class gets its own initialization function for the projectile data
     */
    private Map<Class<?>, VanillaProjectileItemInitializer> vanillaDataMap;
    private Map<Class<?>, ModdedProjectileItemInitializer> moddedDataMap;
    /**
     * Sets of item classes, split along logic differences
     */
    private Set<Class<?>> throwableSet;
    private Set<Class<?>> chargingSet;
    private Set<Class<?>> magazineSet;
    private Set<Class<?>> moddedSet;

    private ItemRegistryService() {}

    public static ItemRegistryService getInstance() {
        if (instance == null) {
            instance = new ItemRegistryService();
            instance.init();
        }
        return instance;
    }

    private void init() {
        vanillaDataMap = new HashMap<>();
        vanillaDataMap.put(BowItem.class, VanillaProjectileItemInitializer.Bow());
        vanillaDataMap.put(CrossbowItem.class, VanillaProjectileItemInitializer.Crossbow());
        vanillaDataMap.put(TridentItem.class, VanillaProjectileItemInitializer.Trident());
        vanillaDataMap.put(EggItem.class, VanillaProjectileItemInitializer.Egg());
        vanillaDataMap.put(EnderpearlItem.class, VanillaProjectileItemInitializer.EnderPearl());
        vanillaDataMap.put(ExperienceBottleItem.class, VanillaProjectileItemInitializer.ExperienceBottle());
        vanillaDataMap.put(SplashPotionItem.class, VanillaProjectileItemInitializer.ThrowablePotion());
        vanillaDataMap.put(LingeringPotionItem.class, VanillaProjectileItemInitializer.ThrowablePotion());
        vanillaDataMap.put(SnowballItem.class, VanillaProjectileItemInitializer.Snowball());
        vanillaDataMap.put(FishingRodItem.class, VanillaProjectileItemInitializer.FishingRod());

        moddedDataMap = new HashMap<>();
        throwableSet = new HashSet<>(List.of(EggItem.class, ExperienceBottleItem.class, EnderpearlItem.class, LingeringPotionItem.class,
                SnowballItem.class, ThrowablePotionItem.class, FishingRodItem.class));
        chargingSet = new HashSet<>(List.of(TridentItem.class, BowItem.class));
        magazineSet = new HashSet<>(List.of(CrossbowItem.class));
        moddedSet = new HashSet<>();
    }

    /**
     * Gets the class of a throwable item or usable ranged weapon if it is in this registry's internal item set
     * @param entity firingEntity to check if holding corresponding items
     * @return item's class or null if nonexistent
     */
    public ItemData getProjectileItemClassIfHeldByEntity(Player entity) {
        ItemStack mainHandStack = entity.getMainHandItem();
        Class<?> mainHandClass = getItemClassFromRegistry(mainHandStack.getItem());
        if (mainHandClass != null) {
            return ItemData.of(mainHandClass, mainHandStack);
        }

        ItemStack offHandStack = entity.getOffhandItem();
        Class<?> offHandClass = getItemClassFromRegistry(offHandStack.getItem());
        if (offHandClass != null) {
            return ItemData.of(offHandClass, offHandStack);
        }
        return null;
    }

    /**
     * <p> Get an item's class from the registry </p>
     * <p> Used instead of .getClass() function to filter out new unhandled items,
     * avoids breaking future versions of Minecraft </p>
     *
     * @param item item to get the class for
     */
    private Class<?> getItemClassFromRegistry(Item item) {
        for (Class<?> clas : Stream.concat(vanillaDataMap.keySet().stream(), moddedDataMap.keySet().stream()).toList()) {
            if (clas.isInstance(item)) {
                return clas;
            }
        }
        return null;
    }

    /**
     * @return if mod should estimate projectile trajectory estimation line
     */
    public boolean shouldDisplayLine(Player firingEntity, Class<?> clas) {
        if (throwableSet.contains(clas)) {
            return true;
        }
        if (chargingSet.contains(clas)) {
            return firingEntity.isUsingItem();
        }
        return magazineSet.contains(clas);
    }

    /**
     * <p> Returns camera offset for the currently held item</p>
     * <p> Used in this mod to render lines skewed a bit to the side</p>
     * <p> Needed due to the way item offset is handled internally </p>
     *
     * @param firingEntity firingEntity holding item
     * @param isPlayer is current client
     * @param itemData currently held item info
     *
     * @return offset as {@link Vec3 Vec3d(x,y,z)}
     *
     * @see net.minecraft.world.entity.Entity#getHandHoldingItemAngle(Item)
     */
    public Vec3 getFirstPersonCameraOffset(Player firingEntity, boolean isPlayer, ItemData itemData) {
        double multiplier = itemData.getItemClass().equals(FishingRodItem.class) ? 0.2f : 1.0f;
        return itemData.getItem() != null && isPlayer ? firingEntity.getHandHoldingItemAngle(itemData.getItem()).scale(multiplier) : Vec3.ZERO;
    }

    /**
     * Loads projectile firingEntity data for estimation
     * @param tickProgress needed for interpolation
     * @param firingEntity needed for camera rotation vector and velocity
     * @param heldItem contains item data, needed to get enchantment data or loaded projectile data
     * @return returns null if can't find data
     * @see VanillaProjectileItemInitializer
     */
    public List<ProjectileData> getProjectileDataFromHeldItem(float tickProgress, Player firingEntity, ItemData heldItem) {
        if (!moddedSet.contains(heldItem.getItemClass())) {
            return vanillaDataMap.getOrDefault(heldItem.getItemClass(), VanillaProjectileItemInitializer.Null()).init(tickProgress, firingEntity, heldItem);
        } else {
            return moddedDataMap.getOrDefault(heldItem.getItemClass(), ModdedProjectileItemInitializer.Null()).init(tickProgress, firingEntity, heldItem);
        }
    }

    /**
     * Adds mod item to {@link ItemRegistryService} internal item set
     *
     * @param itemClass   class of item to add to internal set
     * @param needsCharge needs to be charged to fire (like vanilla bows/tridents)
     */
    public void addModItemClass(Class<?> itemClass, boolean needsCharge, boolean hasCharges) {
        if (needsCharge) {
            chargingSet.add(itemClass);
        } else if (hasCharges) {
            magazineSet.add(itemClass);
        } else {
            throwableSet.add(itemClass);
        }
        moddedSet.add(itemClass);
    }
}
