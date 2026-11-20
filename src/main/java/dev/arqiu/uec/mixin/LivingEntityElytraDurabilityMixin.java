package dev.arqiu.uec.mixin;

import dev.arqiu.uec.component.ElytraChestplateState;
import dev.arqiu.uec.durability.ElytraDurability;
import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraDurabilityMixin {
   @Redirect(
      method = "updateFallFlying",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
      )
   )
   private void uec$flightWear(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
      ElytraDurability.damage(stack, entity, slot, amount, true);
   }

   @Redirect(
      method = "doHurtEquipment",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
      )
   )
   private void uec$combatWear(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
      ElytraDurability.damage(stack, entity, slot, amount, false);
   }

   @Inject(method = "canGlideUsing", at = @At("HEAD"), cancellable = true)
   private static void uec$glideRequiresWings(ItemStack stack, EquipmentSlot slot, CallbackInfoReturnable<Boolean> cir) {
      ElytraChestplateState state = ElytraChestplateState.read(stack).orElse(null);
      if (state != null) {
         boolean gliderAlive = ElytraChestplate.split(stack.getDamageValue(), state)[1] < state.gliderMaxDamage();
         cir.setReturnValue(slot == EquipmentSlot.CHEST && stack.has(DataComponents.GLIDER) && gliderAlive);
      }
   }
}
