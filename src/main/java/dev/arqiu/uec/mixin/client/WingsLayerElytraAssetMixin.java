package dev.arqiu.uec.mixin.client;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WingsLayer.class)
public abstract class WingsLayerElytraAssetMixin {
   @ModifyVariable(
      method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
      at = @At("STORE"),
      ordinal = 0
   )
   private ItemStack uec$renderWingsFromGlider(ItemStack chestEquipment) {
      ItemStack glider = ElytraChestplate.displayGlider(chestEquipment);
      return glider.isEmpty() ? chestEquipment : glider;
   }
}
