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

// NeoForge 26.3 版：滑翔掉耐久在 26.3 的 NeoForge 补丁里被从
// LivingEntity.updateFallFlying -> ItemStack.hurtAndBreak 改成了
// LivingEntity.updateFallFlying -> ItemStack.onGlideDamage（转调 Item.onGlideDamage，默认实现才是 hurtAndBreak），
// 所以这里的 @Redirect 换成拦 onGlideDamage 的调用点。
// 非融合胸甲仍然走 stack.onGlideDamage，保留其它模组对 Item.onGlideDamage 的自定义实现。
@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraDurabilityMixin {
   @Redirect(
      method = "updateFallFlying",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;onGlideDamage(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
      )
   )
   private void uec$flightWear(ItemStack stack, LivingEntity entity, EquipmentSlot slot) {
      if (ElytraChestplateState.read(stack).isEmpty()) {
         stack.onGlideDamage(entity, slot);
      } else {
         ElytraDurability.damage(stack, entity, slot, 1, true);
      }
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
