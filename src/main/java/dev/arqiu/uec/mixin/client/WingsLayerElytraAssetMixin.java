package dev.arqiu.uec.mixin.client;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.client.renderer.entity.layers.WingsLayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 双模型渲染（客户端）：合并胸甲穿着时按原版路径渲染鞘翅。
 * <p>原版 {@code WingsLayer.submit} 开头把 {@code state.chestEquipment} 存入局部变量，
 * 之后的模型选择、装备资产查询（EQUIPPABLE.assetId）、纹饰 / 染色 / 附魔光泽
 * 与皮肤纹理（getPlayerElytraTexture）全部围绕这个变量展开；胸甲自身装备资产的
 * WINGS 层为空，因此穿合并胸甲时原版什么都不画，而若只替换资产键又会把胸甲的
 * 纹饰 / 染色带进鞘翅层（生成失败即紫黑缺失纹理）。</p>
 * <p>这里改用「换堆」思路：在该变量写入后立即替换为按记账数据构造的<b>鞘翅展示堆</b>
 * （物品 id 来自 {@code minecraft:custom_data} 标量字段，附魔复制自合并堆以保证光泽），
 * 使整个渲染流程与穿着真鞘翅完全一致——鞘翅模型、动态皮肤纹理、附魔光泽、
 * 无纹饰 / 染色干扰；胸甲本体仍由 {@code HumanoidArmorLayer} 照常渲染。</p>
 * <p>未安装本模组的客户端不会应用该 mixin：穿着合并胸甲时看不到翅膀，
 * 但飞行能力本身由服务端裁决（装备堆带原版 GLIDER 组件），不受影响。</p>
 */
@Mixin(WingsLayer.class)
public abstract class WingsLayerElytraAssetMixin {

    @ModifyVariable(
            method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/HumanoidRenderState;FF)V",
            at = @At("STORE"),
            ordinal = 0)
    private ItemStack uec$renderWingsFromGlider(ItemStack chestEquipment) {
        ItemStack glider = ElytraChestplate.displayGlider(chestEquipment);
        return glider.isEmpty() ? chestEquipment : glider; // 非合并胸甲 / 数据损坏：原样走原版逻辑。
    }
}
