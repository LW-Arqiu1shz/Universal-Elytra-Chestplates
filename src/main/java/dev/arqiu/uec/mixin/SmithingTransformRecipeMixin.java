package dev.arqiu.uec.mixin;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SmithingTransformRecipe.class)
public abstract class SmithingTransformRecipeMixin {
   @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SmithingRecipeInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("RETURN"))
   private void uec$refreshMergedItem(SmithingRecipeInput input, CallbackInfoReturnable<ItemStack> cir) {
      ElytraChestplate.applyCurrentItem((ItemStack)cir.getReturnValue());
   }
}
