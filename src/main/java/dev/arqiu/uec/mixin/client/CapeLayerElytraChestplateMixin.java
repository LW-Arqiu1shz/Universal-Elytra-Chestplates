package dev.arqiu.uec.mixin.client;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo.LayerType;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CapeLayer.class)
public abstract class CapeLayerElytraChestplateMixin {
   @Inject(method = "hasLayer", at = @At("HEAD"), cancellable = true)
   private void uec$mergedWingsSuppressCape(ItemStack chestEquipment, LayerType layerType, CallbackInfoReturnable<Boolean> cir) {
      if (layerType == LayerType.WINGS && ElytraChestplate.isMerged(chestEquipment)) {
         cir.setReturnValue(true);
      }
   }
}
