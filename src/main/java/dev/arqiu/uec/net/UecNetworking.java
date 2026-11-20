package dev.arqiu.uec.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

public final class UecNetworking {
   private UecNetworking() {
   }

   public static void init() {
      PayloadTypeRegistry.serverboundPlay().register(CancelGlidePayload.TYPE, CancelGlidePayload.CODEC);
      ServerPlayNetworking.registerGlobalReceiver(CancelGlidePayload.TYPE, (payload, context) -> {
         ServerPlayer player = context.player();
         if (player.isFallFlying()) {
            player.stopFallFlying();
         }
      });
   }
}
