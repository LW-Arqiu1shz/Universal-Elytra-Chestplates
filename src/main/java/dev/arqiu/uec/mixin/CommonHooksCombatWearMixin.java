package dev.arqiu.uec.mixin;

import dev.arqiu.uec.durability.ElytraDurability;
import net.neoforged.neoforge.common.CommonHooks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// NeoForge 版专用：原版 doHurtEquipment 里的 ItemStack.hurtAndBreak 调用被 NeoForge
// 挪到了 CommonHooks.onArmorHurt 事后的 lambda 里，所以战斗磨损的重定向改成注入这里。
// 效果与 Fabric 版重定向 LivingEntity.doHurtEquipment 一致，且仍沿用事件调整后的伤害值。
// require = 0：lambda 编号换个 NeoForge 小版本可能会变，挂不上时只是战斗磨损按原版算，不会启动崩溃。
@Mixin(CommonHooks.class)
public abstract class CommonHooksCombatWearMixin {
   @Redirect(
      method = "lambda$onArmorHurt$0",
      require = 0,
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"
      )
   )
   private static void uec$combatWear(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
      ElytraDurability.damage(stack, entity, slot, amount, false);
   }
}
