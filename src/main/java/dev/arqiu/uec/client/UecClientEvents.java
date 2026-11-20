package dev.arqiu.uec.client;

import dev.arqiu.uec.UniversalElytraChestplates;
import dev.arqiu.uec.component.ElytraChestplateState;
import dev.arqiu.uec.equipment.ElytraChestplate;
import dev.arqiu.uec.net.CancelGlidePayload;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(
   modid = UniversalElytraChestplates.MOD_ID,
   value = Dist.CLIENT
)
public final class UecClientEvents {
   private static boolean sprintWasDown;

   private UecClientEvents() {
   }

   @SubscribeEvent
   public static void onClientTick(ClientTickEvent.Post event) {
      Minecraft mc = Minecraft.getInstance();
      boolean down = mc.options.keySprint.isDown();
      if (down && !sprintWasDown && mc.player != null && mc.player.isFallFlying() && !mc.player.onGround()) {
         // 服务端不认这个包时，NeoForge 发包会直接抛异常，所以先确认服务端能收再发。
         ClientPacketListener connection = mc.getConnection();
         if (connection != null && connection.hasChannel(CancelGlidePayload.TYPE)) {
            ClientPacketDistributor.sendToServer(CancelGlidePayload.INSTANCE);
         }
      }

      sprintWasDown = down;
   }

   @SubscribeEvent
   public static void onItemTooltip(ItemTooltipEvent event) {
      appendElytraTooltip(event.getItemStack(), event.getContext(), event.getFlags(), event.getToolTip());
   }

   private static void appendElytraTooltip(ItemStack stack, TooltipContext context, TooltipFlag flag, List<Component> lines) {
      ElytraChestplateState state = ElytraChestplateState.read(stack).orElse(null);
      if (state == null) {
         if (ElytraChestplate.isGlider(stack)) {
            lines.add(Component.translatable("item.universal_elytra_chestplates.tooltip.merge_hint").withStyle(ChatFormatting.GRAY));
         }
      } else {
         int[] split = ElytraChestplate.split(stack.getDamageValue(), state);
         int gliderLeft = Math.max(0, state.gliderMaxDamage() - split[1]);
         lines.add(
            Component.translatable("item.universal_elytra_chestplates.tooltip.glider", new Object[]{gliderLeft, state.gliderMaxDamage()})
               .withStyle(ChatFormatting.BLUE)
         );
         lines.add(Component.translatable("item.universal_elytra_chestplates.tooltip.split_hint").withStyle(ChatFormatting.GRAY));
      }
   }
}
