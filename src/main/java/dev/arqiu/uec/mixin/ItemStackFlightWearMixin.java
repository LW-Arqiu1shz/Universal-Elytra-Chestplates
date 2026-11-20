package dev.arqiu.uec.mixin;

import dev.arqiu.uec.durability.FlightWearContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemStack.class)
public abstract class ItemStackFlightWearMixin {
   @Redirect(
      method = "processDurabilityChange",
      at = @At(
         value = "INVOKE",
         target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;processDurabilityChange(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;I)I"
      )
   )
   private int uec$evaluateFlightWearAsGlider(ServerLevel level, ItemStack stack, int amount) {
      ItemStack proxy = FlightWearContext.proxyFor(stack);
      return EnchantmentHelper.processDurabilityChange(level, proxy != null ? proxy : stack, amount);
   }
}
