package dev.arqiu.uec.equipment;

import dev.arqiu.uec.component.ElytraChestplateState;
import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.equipment.Equippable;

public final class ElytraChestplate {
   private ElytraChestplate() {
   }

   public static boolean isMerged(ItemStack stack) {
      return ElytraChestplateState.has(stack);
   }

   public static boolean isChestArmorItem(Item item) {
      Equippable equippable = (Equippable)item.components().get(DataComponents.EQUIPPABLE);
      return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
   }

   public static boolean canMergeInto(ItemStack stack) {
      return !stack.isEmpty() && isChestArmorItem(stack.getItem()) && !isMerged(stack) && !stack.has(DataComponents.GLIDER);
   }

   public static boolean isGlider(ItemStack stack) {
      if (!stack.isEmpty() && !isMerged(stack) && stack.has(DataComponents.GLIDER)) {
         Equippable equippable = (Equippable)stack.get(DataComponents.EQUIPPABLE);
         return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
      } else {
         return false;
      }
   }

   public static Optional<ItemStack> merge(ItemStack chest, ItemStack glider, Provider provider) {
      int gliderDamage = glider.getDamageValue();
      int gliderMax = glider.getMaxDamage();
      Optional<Tag> gliderSnapshot = ElytraChestplateState.encodeSnapshot(snapshot(glider), provider);
      if (gliderSnapshot.isEmpty()) {
         return Optional.empty();
      }

      ItemStack merged = chest.copyWithCount(1);
      merged.set(DataComponents.MAX_DAMAGE, chest.getMaxDamage() + gliderMax);
      merged.setDamageValue(chest.getDamageValue() + gliderDamage);
      merged.set(DataComponents.GLIDER, Unit.INSTANCE);
      merged.set(
         DataComponents.CUSTOM_NAME,
         combinedName(chest.getHoverName(), glider.getHoverName(), chest.has(DataComponents.CUSTOM_NAME) || glider.has(DataComponents.CUSTOM_NAME))
      );
      Equippable equippable = (Equippable)merged.get(DataComponents.EQUIPPABLE);
      if (equippable != null && !equippable.swappable()) {
         merged.set(DataComponents.EQUIPPABLE, withSwappable(equippable, true));
      }

      ElytraChestplateState.write(
         merged,
         new ElytraChestplateState(
            id(glider.getItem()),
            gliderDamage,
            gliderMax,
            ElytraChestplateState.encodeName(unstyled(glider.getHoverName())),
            ElytraChestplateState.encodeName((Component)chest.get(DataComponents.CUSTOM_NAME)),
            chest.getMaxDamage(),
            !((ItemEnchantments)glider.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)).isEmpty(),
            chest.get(DataComponents.EQUIPPABLE) != null && ((Equippable)chest.get(DataComponents.EQUIPPABLE)).swappable()
         ),
         gliderSnapshot.get()
      );
      return Optional.of(merged);
   }

   public static void applyCurrentItem(ItemStack merged) {
      ElytraChestplateState state = ElytraChestplateState.read(merged).orElse(null);
      if (state != null) {
         Integer itemMax = (Integer)merged.getItem().components().get(DataComponents.MAX_DAMAGE);
         int currentChestMax = itemMax == null ? 0 : Math.max(0, itemMax);
         if (currentChestMax + state.gliderMaxDamage() > 0) {
            merged.set(DataComponents.MAX_DAMAGE, currentChestMax + state.gliderMaxDamage());
         } else {
            merged.remove(DataComponents.MAX_DAMAGE);
         }

         ElytraChestplateState.updateChestMaxDamage(merged, currentChestMax);
         Equippable prototype = (Equippable)merged.getItem().components().get(DataComponents.EQUIPPABLE);
         if (prototype != null) {
            merged.set(DataComponents.EQUIPPABLE, withSwappable(prototype, true));
            ElytraChestplateState.updateChestSwappable(merged, prototype.swappable());
         }

         Optional<Component> gliderName = ElytraChestplateState.decodeName(state.gliderName());
         if (gliderName.isPresent()) {
            Component current = (Component)merged.get(DataComponents.CUSTOM_NAME);
            boolean italic = current != null && current.getStyle().isItalic();
            Component chestName = ElytraChestplateState.decodeName(state.chestName())
               .orElseGet(() -> Component.translatable(merged.getItem().getDescriptionId()));
            merged.set(DataComponents.CUSTOM_NAME, combinedName(chestName, gliderName.get(), italic));
         }
      }
   }

   public static Optional<ItemStack> restoredChest(ItemStack merged) {
      Optional<ElytraChestplateState> stateOpt = ElytraChestplateState.read(merged);
      if (stateOpt.isEmpty()) {
         return Optional.empty();
      }

      ElytraChestplateState state = stateOpt.get();
      ItemStack chest = merged.copyWithCount(1);
      chest.remove(DataComponents.GLIDER);
      int chestMax = state.chestMaxDamage() >= 0 ? state.chestMaxDamage() : legacyChestMaxDamage(merged, state);
      if (chestMax > 0) {
         chest.set(DataComponents.MAX_DAMAGE, chestMax);
      } else {
         chest.remove(DataComponents.MAX_DAMAGE);
      }

      Equippable restoredEquippable = (Equippable)chest.get(DataComponents.EQUIPPABLE);
      if (restoredEquippable != null) {
         chest.set(DataComponents.EQUIPPABLE, withSwappable(restoredEquippable, state.chestOriginalSwappable()));
      }

      ElytraChestplateState.clear(chest);
      ElytraChestplateState.decodeName(state.chestName())
         .ifPresentOrElse(name -> chest.set(DataComponents.CUSTOM_NAME, name), () -> chest.remove(DataComponents.CUSTOM_NAME));
      chest.setDamageValue(split(merged.getDamageValue(), state)[0]);
      return Optional.of(chest);
   }

   private static int legacyChestMaxDamage(ItemStack merged, ElytraChestplateState state) {
      Integer prototype = (Integer)merged.getItem().components().get(DataComponents.MAX_DAMAGE);
      if (prototype != null && prototype > 0) {
         int inferred = merged.getMaxDamage() - Math.max(0, state.gliderMaxDamage());
         return inferred > 0 ? inferred : prototype;
      } else {
         return 0;
      }
   }

   public static Optional<ItemStack> restoredGlider(ItemStack merged, Provider provider) {
      Optional<ElytraChestplateState> stateOpt = ElytraChestplateState.read(merged);
      if (stateOpt.isEmpty()) {
         return Optional.empty();
      }

      ElytraChestplateState state = stateOpt.get();
      Optional<ItemStack> base = ElytraChestplateState.readSnapshot(merged, provider).or(() -> {
         Item item = itemById(state.gliderItem());
         return item == null ? Optional.empty() : Optional.of(new ItemStack(item));
      });
      return base.map(part -> {
         ItemStack glider = part.copyWithCount(1);
         glider.set(DataComponents.MAX_DAMAGE, state.gliderMaxDamage());
         glider.setDamageValue(split(merged.getDamageValue(), state)[1]);
         return glider;
      });
   }

   public static ItemStack displayGlider(ItemStack merged) {
      ElytraChestplateState state = ElytraChestplateState.read(merged).orElse(null);
      if (state == null) {
         return ItemStack.EMPTY;
      }

      Item gliderItem = itemById(state.gliderItem());
      if (gliderItem == null) {
         return ItemStack.EMPTY;
      }

      ItemStack display = new ItemStack(gliderItem);
      if (state.gliderHasEnchantments()) {
         display.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
      }

      return display;
   }

   public static int[] split(int totalDamage, ElytraChestplateState state) {
      int total = Math.max(totalDamage, 0);
      int gliderPart = Mth.clamp(state.gliderDamage(), 0, state.gliderMaxDamage());
      if (gliderPart > total) {
         gliderPart = total;
      }

      return new int[]{total - gliderPart, gliderPart};
   }

   private static ItemStack snapshot(ItemStack stack) {
      ItemStack copy = stack.copyWithCount(1);
      copy.remove(DataComponents.DAMAGE);
      copy.remove(DataComponents.MAX_DAMAGE);
      return copy;
   }

   private static String id(Item item) {
      Identifier key = BuiltInRegistries.ITEM.getKey(item);
      return key == null ? "" : key.toString();
   }

   private static Item itemById(String id) {
      try {
         return (Item)BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
      } catch (RuntimeException e) {
         return null;
      }
   }

   private static Component combinedName(Component chestName, Component gliderName, boolean renamed) {
      MutableComponent name = Component.translatableWithFallback(
         "item.universal_elytra_chestplates.name.merged", "%1$s with %2$s", new Object[]{unstyled(chestName), unstyled(gliderName)}
      );
      name.setStyle(Style.EMPTY.withItalic(renamed));
      return name;
   }

   private static Equippable withSwappable(Equippable source, boolean swappable) {
      return new Equippable(
         source.slot(),
         source.equipSound(),
         source.assetId(),
         source.cameraOverlay(),
         source.allowedEntities(),
         source.dispensable(),
         swappable,
         source.damageOnHurt(),
         source.equipOnInteract(),
         source.canBeSheared(),
         source.shearingSound()
      );
   }

   private static Component unstyled(Component part) {
      MutableComponent copy = part.copy();
      copy.setStyle(Style.EMPTY);
      return copy;
   }
}
