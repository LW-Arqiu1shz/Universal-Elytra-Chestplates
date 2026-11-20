package dev.arqiu.uec;

import dev.arqiu.uec.interaction.UecInteractions;
import dev.arqiu.uec.net.UecNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

public final class UniversalElytraChestplates implements ModInitializer {
   public static final String MOD_ID = "universal_elytra_chestplates";

   public static Identifier id(String path) {
      return Identifier.fromNamespaceAndPath("universal_elytra_chestplates", path);
   }

   public void onInitialize() {
      UecInteractions.init();
      UecNetworking.init();
   }
}
