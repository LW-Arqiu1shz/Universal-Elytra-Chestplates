package dev.arqiu.uec;

import net.minecraft.resources.Identifier;
import net.neoforged.fml.common.Mod;

@Mod(UniversalElytraChestplates.MOD_ID)
public final class UniversalElytraChestplates {
   public static final String MOD_ID = "universal_elytra_chestplates";

   // NeoForge 要求 @Mod 类恰好有一个公开构造器；事件注册全部走 @EventBusSubscriber。
   public UniversalElytraChestplates() {
   }

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("universal_elytra_chestplates", path);
   }
}
