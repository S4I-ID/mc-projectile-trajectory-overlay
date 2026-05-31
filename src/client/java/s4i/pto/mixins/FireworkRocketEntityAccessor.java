package s4i.pto.mixins;

import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.component.FireworkExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Accessor("lifetime")
    int getLifetime();

    @Accessor("life")
    int getLife();

    @Invoker("getExplosions")
    List<FireworkExplosion> getExplosiveCharges();
}
