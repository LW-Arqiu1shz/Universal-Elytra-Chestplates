package dev.arqiu.uec.mixin;

import dev.arqiu.uec.interaction.UecTableFusion;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityTableFusionMixin {
   @Inject(method = "tick", at = @At("TAIL"))
   private void uec$fuseOnSmithingTable(CallbackInfo ci) {
      UecTableFusion.onItemTick((ItemEntity)(Object)this);
   }
}
