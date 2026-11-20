package dev.arqiu.uec.mixin;

import dev.arqiu.uec.interaction.UecInteractions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Equippable.class)
public abstract class EquippableChestResyncMixin {
   @Inject(method = "swapWithEquipmentSlot", at = @At("TAIL"))
   private void uec$resyncChestSlot(ItemStack stack, Player player, CallbackInfoReturnable<InteractionResult> cir) {
      if (player instanceof ServerPlayer serverPlayer && ((Equippable)(Object)this).slot() == EquipmentSlot.CHEST) {
         UecInteractions.resyncChestEquipment(serverPlayer);
      }
   }
}
