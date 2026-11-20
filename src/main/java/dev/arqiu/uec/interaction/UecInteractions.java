package dev.arqiu.uec.interaction;

import com.mojang.datafixers.util.Pair;
import dev.arqiu.uec.UniversalElytraChestplates;
import dev.arqiu.uec.equipment.ElytraChestplate;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.util.Prediction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult.Success;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = UniversalElytraChestplates.MOD_ID)
public final class UecInteractions {
   private UecInteractions() {
   }

   @SubscribeEvent
   public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
      Level level = event.getLevel();
      if (!level.isClientSide()) {
         InteractionResult result = onUseItem(event.getEntity(), level, event.getHand());
         if (result != InteractionResult.PASS) {
            event.setCancellationResult(result);
            event.setCanceled(true);
         }
      }
   }

   private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
      if (level.isClientSide()) {
         return InteractionResult.PASS;
      } else {
         ItemStack held = player.getItemInHand(hand);
         if (ElytraChestplate.isMerged(held)) {
            Provider provider = level.registryAccess();
            return player.isSecondaryUseActive() ? trySplit(player, hand, provider) : tryWear(player, hand, held);
         } else {
            return InteractionResult.PASS;
         }
      }
   }

   private static InteractionResult tryWear(Player player, InteractionHand hand, ItemStack held) {
      Equippable equippable = (Equippable)held.get(DataComponents.EQUIPPABLE);
      if (equippable == null) {
         return InteractionResult.PASS;
      }

      InteractionResult result = equippable.swapWithEquipmentSlot(held, player);
      if (result instanceof Success success) {
         ItemStack transformed = success.heldItemTransformedTo();
         if (transformed != null && transformed != held && !transformed.isEmpty()) {
            player.setItemInHand(hand, transformed);
         }

         player.containerMenu.broadcastChanges();
         resyncChestEquipment(player);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return result;
      }
   }

   private static InteractionResult trySplit(Player player, InteractionHand hand, Provider provider) {
      ItemStack merged = player.getItemInHand(hand);
      Optional<ItemStack> chest = ElytraChestplate.restoredChest(merged);
      Optional<ItemStack> glider = ElytraChestplate.restoredGlider(merged, provider);
      if (!chest.isEmpty() && !glider.isEmpty()) {
         player.setItemSlot(hand.asEquipmentSlot(), chest.get());
         InteractionHand otherHand = opposite(hand);
         if (player.getItemInHand(otherHand).isEmpty()) {
            player.setItemSlot(otherHand.asEquipmentSlot(), glider.get());
         } else if (!player.getInventory().add(glider.get())) {
            player.drop(glider.get(), false, Prediction.SERVER_ONLY);
         }

         player.containerMenu.broadcastChanges();
         resyncChestEquipment(player);
         return InteractionResult.SUCCESS_SERVER;
      } else {
         return InteractionResult.PASS;
      }
   }

   public static void resyncChestEquipment(Player player) {
      if (player instanceof ServerPlayer serverPlayer) {
         serverPlayer.connection
            .send(
               new ClientboundSetEquipmentPacket(serverPlayer.getId(), List.of(Pair.of(EquipmentSlot.CHEST, serverPlayer.getItemBySlot(EquipmentSlot.CHEST))))
            );
      }
   }

   private static InteractionHand opposite(InteractionHand hand) {
      return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
   }
}
