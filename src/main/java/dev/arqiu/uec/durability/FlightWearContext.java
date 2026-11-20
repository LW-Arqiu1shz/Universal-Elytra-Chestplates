package dev.arqiu.uec.durability;

import net.minecraft.world.item.ItemStack;

public final class FlightWearContext {
   private static final ThreadLocal<ItemStack[]> CURRENT = new ThreadLocal<>();

   private FlightWearContext() {
   }

   public static void begin(ItemStack target, ItemStack proxy) {
      CURRENT.set(new ItemStack[]{target, proxy});
   }

   public static void end() {
      CURRENT.remove();
   }

   public static ItemStack proxyFor(ItemStack stack) {
      ItemStack[] pair = CURRENT.get();
      return pair != null && pair[0] == stack ? pair[1] : null;
   }
}
