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

/**
 * 任何玩家经原版右键换装（{@code Equippable#swapWithEquipmentSlot}）后，
 * 向当事玩家补发一条胸甲槽装备同步包。
 * <p>胸甲槽内容在容器未打开时不会随背包同步，原版右键穿戴靠客户端本地预测自洽；
 * 但本模组的合并堆 {@code swappable=false}、客户端不预测——快速连点时
 * 「合并胸甲 ↔ 普通胸甲」两条路径交替，客户端盔甲槽会停留在上一次的预测残留上，
 * 表现为幽灵鞘翅虚假顶掉胸甲。统一在换装落账后立即纠正当事客户端，彻底消除残留。</p>
 */
@Mixin(Equippable.class)
public abstract class EquippableChestResyncMixin {

    @Inject(method = "swapWithEquipmentSlot", at = @At("TAIL"))
    private void uec$resyncChestSlot(ItemStack stack, Player player,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        if (player instanceof ServerPlayer serverPlayer
                && ((Equippable) (Object) this).slot() == EquipmentSlot.CHEST) {
            UecInteractions.resyncChestEquipment(serverPlayer);
        }
    }
}
