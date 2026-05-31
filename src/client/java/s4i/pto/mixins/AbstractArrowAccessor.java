package s4i.pto.mixins;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractArrow.class)
public interface AbstractArrowAccessor {
    @Invoker("isInGround")
    boolean isStopped();
}
