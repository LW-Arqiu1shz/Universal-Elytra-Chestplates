package dev.arqiu.uec.mixin;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 锻造台升级（钻石胸甲 → 下界合金胸甲等）后的合并堆修正。
 * <p>原版 {@code TransmuteRecipe#createWithOriginalComponents} 会把底材的全部组件
 * 原样搬到新物品上，合并堆里「随胸甲物品而变」的部分——耐久上限、装备属性
 * （盔甲模型资产 / 可右键直穿）与组合显示名称——因此仍停在旧胸甲上。
 * 这里在组装结果返回后把它们拉回当前物品（见 {@link ElytraChestplate#applyCurrentItem}），
 * 不带本模组记账的普通物品完全不受影响。</p>
 */
@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void uec$refreshMergedItem(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> cir) {
        ElytraChestplate.applyCurrentItem(cir.getReturnValue());
    }
}
