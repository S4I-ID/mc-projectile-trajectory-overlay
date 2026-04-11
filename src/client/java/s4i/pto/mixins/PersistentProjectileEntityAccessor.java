package s4i.pto.mixins;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = PersistentProjectileEntity.class)
public interface PersistentProjectileEntityAccessor {
    @Invoker("isInGround")
    boolean isStopped();
}
