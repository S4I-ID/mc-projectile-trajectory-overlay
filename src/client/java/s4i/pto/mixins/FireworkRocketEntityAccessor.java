package s4i.pto.mixins;

import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketEntityAccessor {
    @Accessor("lifeTime")
    int getLifeTime();

    @Accessor("life")
    int getLife();

    @Invoker("getExplosions")
    List<FireworkExplosionComponent> getExplosiveCharges();
}
