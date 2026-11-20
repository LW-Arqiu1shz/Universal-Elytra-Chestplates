package dev.arqiu.uec.mixin.client;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 穿着合并胸甲时隐藏披风。
 * <p>原版 {@code CapeLayer} 在胸甲装备的资产定义了 WINGS 层（即真鞘翅）时不画披风
 * （披风纹理转而由鞘翅模型使用）。合并胸甲的资产是普通胸甲、没有 WINGS 层，
 * 导致披风与鞘翅同时出现。这里对合并胸甲按「有 WINGS 层」处理，与穿真鞘翅一致。</p>
 */
@Mixin(CapeLayer.class)
public abstract class CapeLayerElytraChestplateMixin {

    @Inject(method = "hasLayer", at = @At("HEAD"), cancellable = true)
    private void uec$mergedWingsSuppressCape(ItemStack chestEquipment, EquipmentClientInfo.LayerType layerType,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (layerType == EquipmentClientInfo.LayerType.WINGS && ElytraChestplate.isMerged(chestEquipment)) {
            cir.setReturnValue(true);
        }
    }
}
