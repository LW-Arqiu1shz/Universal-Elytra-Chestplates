package dev.arqiu.uec.mixin;

import dev.arqiu.uec.durability.FlightWearContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 飞行磨损的附魔判定改按鞘翅展示堆（服务端）。
 * <p>原版 {@code ItemStack#processDurabilityChange} 里唯一的附魔介入点是
 * {@code EnchantmentHelper#processDurabilityChange(level, stack, amount)}，
 * 它按堆的<b>物品身份</b>评估 {@code minecraft:item_damage} 效果（如 Unbreaking
 * 在 {@code enchantable/armor} 标签内外分档，鞘翅档保护远强于胸甲档）。
 * 这里只在 {@link FlightWearContext} 飞行损耗窗口打开、且被损伤堆正是窗口目标堆时，
 * 把判定堆换成等价附魔的鞘翅展示堆——伤害是否落地、落地多少仍由原版随机决定，
 * 创造免损、损毁回调、进度触发等全部不受影响。窗口外的一切损伤
 * （受击磨损、其它装备、其它物品）原样走原版。</p>
 */
@Mixin(ItemStack.class)
public abstract class ItemStackFlightWearMixin {

    @Redirect(method = "processDurabilityChange",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"))
    private int uec$evaluateFlightWearAsGlider(ServerLevel level, ItemStack stack, int amount) {
        ItemStack proxy = FlightWearContext.proxyFor(stack);
        return EnchantmentHelper.processDurabilityChange(level, proxy != null ? proxy : stack, amount);
    }
}
