package dev.arqiu.uec.net;

import dev.arqiu.uec.UniversalElytraChestplates;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = UniversalElytraChestplates.MOD_ID)
public final class UecNetworking {
   private UecNetworking() {
   }

   // RegisterPayloadHandlersEvent 是 mod 总线事件，FML 会按参数类型自动挂到 mod 总线。
   @SubscribeEvent
   public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
      // optional()：服务端没装本模组（原版 / Fabric / 没装 UEC 的 NeoForge 服务器）时照样能进服，
      // 只是取消滑翔不生效。装了 UEC Fabric 版的 Fabric 服务器会通过 minecraft:register 声明
      // 它能收 universal_elytra_chestplates:cancel_glide（包体为空），两边格式一致，可以互通。
      PayloadRegistrar registrar = event.registrar("1").optional();
      registrar.playToServer(CancelGlidePayload.TYPE, CancelGlidePayload.CODEC, UecNetworking::onCancelGlide);
   }

   private static void onCancelGlide(CancelGlidePayload payload, IPayloadContext context) {
      Player player = context.player();
      if (player.isFallFlying()) {
         player.stopFallFlying();
      }
   }
}
