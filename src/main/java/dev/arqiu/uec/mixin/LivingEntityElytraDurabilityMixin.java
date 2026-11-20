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

/**
 * 双重耐久的损耗归属与飞行准入（服务端）。
 * <p>原版 26.3 中：飞行磨损在 {@code LivingEntity#updateFallFlying}（对可用的滑翔
 * 装备周期调用 {@code ItemStack.hurtAndBreak}），受击磨损在
 * {@code LivingEntity#doHurtEquipment}。这里把这两处的调用替换为
 * {@link ElytraDurability#damage}：仍调用原版方法（保留 Unbreaking、创造免疫、
 * 损毁行为），只把损伤量按「飞行只扣鞘翅部分、受击先扣胸甲部分」记账。</p>
 * <p>另外拦截滑翔准入 {@code LivingEntity#canGlideUsing}：合并胸甲只看<b>鞘翅部分</b>
 * 的耐久——鞘翅部分耗尽即禁飞（哪怕胸甲部分还很完整），胸甲部分耗尽也不影响飞行。</p>
 * <p>@Redirect handler 参数顺序 = 被重定向调用的接收者与实参（外层方法参数如需捕获须排在其后）。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityElytraDurabilityMixin {

    @Redirect(method = "updateFallFlying",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void uec$flightWear(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        ElytraDurability.damage(stack, entity, slot, amount, true);
    }

    @Redirect(method = "doHurtEquipment",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;hurtAndBreak(ILnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/EquipmentSlot;)V"))
    private void uec$combatWear(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        ElytraDurability.damage(stack, entity, slot, amount, false);
    }

    @Inject(method = "canGlideUsing", at = @At("HEAD"), cancellable = true)
    private static void uec$glideRequiresWings(ItemStack stack, EquipmentSlot slot,
                                               CallbackInfoReturnable<Boolean> cir) {
        ElytraChestplateState state = ElytraChestplateState.read(stack).orElse(null);
        if (state == null) {
            return; // 普通装备（或状态损坏）：原版判定。
        }
        // 合并胸甲：鞘翅部分未耗尽才能飞；整条共享耐久条（含胸甲部分）不参与禁飞判定，
        // 胸甲部分只影响防御与损毁。
        boolean gliderAlive = ElytraChestplate.split(stack.getDamageValue(), state)[1] < state.gliderMaxDamage();
        cir.setReturnValue(slot == EquipmentSlot.CHEST && stack.has(DataComponents.GLIDER) && gliderAlive);
    }
}
