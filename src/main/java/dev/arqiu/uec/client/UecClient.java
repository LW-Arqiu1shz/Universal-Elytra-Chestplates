package dev.arqiu.uec.client;

import dev.arqiu.uec.component.ElytraChestplateState;
import dev.arqiu.uec.equipment.ElytraChestplate;
import dev.arqiu.uec.net.CancelGlidePayload;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.Item.TooltipContext;

public final class UecClient implements ClientModInitializer {
   private boolean sprintWasDown;

   public void onInitializeClient() {
      ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
      ItemTooltipCallback.EVENT.register(UecClient::appendElytraTooltip);
   }

   private void onClientTick(Minecraft mc) {
      boolean down = mc.options.keySprint.isDown();
      if (down && !this.sprintWasDown && mc.player != null && mc.player.isFallFlying() && !mc.player.onGround()) {
         ClientPlayNetworking.send(CancelGlidePayload.INSTANCE);
      }

      this.sprintWasDown = down;
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
