package dev.arqiu.uec.component;

import java.util.Optional;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public record ElytraChestplateState(
   String gliderItem,
   int gliderDamage,
   int gliderMaxDamage,
   Tag gliderName,
   Tag chestName,
   int chestMaxDamage,
   boolean gliderHasEnchantments,
   boolean chestOriginalSwappable
) {
   public static final String KEY = "universal_elytra_chestplate";
   public static final String GLIDER_SNAPSHOT = "glider";
   private static final String F_GLIDER_ITEM = "glider_item";
   private static final String F_GLIDER_DAMAGE = "glider_damage";
   private static final String F_GLIDER_MAX = "glider_max_damage";
   private static final String F_GLIDER_NAME = "glider_name";
   private static final String F_CHEST_NAME = "chest_name";
   private static final String F_CHEST_MAX = "chest_max_damage";
   private static final String F_GLIDER_ENCHANTED = "glider_has_enchantments";
   private static final String F_CHEST_SWAPPABLE = "chest_original_swappable";

   public static boolean has(ItemStack stack) {
      if (stack.isEmpty()) {
         return false;
      }

      CustomData custom = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      return custom != null && !custom.isEmpty() ? custom.copyTag().get("universal_elytra_chestplate") instanceof CompoundTag : false;
   }

   public static Optional<ElytraChestplateState> read(ItemStack stack) {
      if (stack.isEmpty()) {
         return Optional.empty();
      } else {
         CustomData custom = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
         if (custom != null && !custom.isEmpty() && custom.copyTag().get("universal_elytra_chestplate") instanceof CompoundTag state) {
            Optional var5 = state.getString("glider_item");
            Optional gliderMax = state.getInt("glider_max_damage");
            return !var5.isEmpty() && !((String)var5.get()).isEmpty() && !gliderMax.isEmpty()
               ? Optional.of(
                  new ElytraChestplateState(
                     (String)var5.get(),
                     state.getIntOr("glider_damage", 0),
                     (Integer)gliderMax.get(),
                     state.get("glider_name"),
                     state.get("chest_name"),
                     state.getIntOr("chest_max_damage", -1),
                     state.getBooleanOr("glider_has_enchantments", false),
                     state.getBooleanOr("chest_original_swappable", true)
                  )
               )
               : Optional.empty();
         } else {
            return Optional.empty();
         }
      }
   }

   public static void write(ItemStack stack, ElytraChestplateState state, Tag gliderSnapshot) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
         CompoundTag compound = new CompoundTag();
         compound.putString("glider_item", state.gliderItem());
         compound.putInt("glider_damage", state.gliderDamage());
         compound.putInt("glider_max_damage", state.gliderMaxDamage());
         putIfPresent(compound, "glider_name", state.gliderName());
         putIfPresent(compound, "chest_name", state.chestName());
         compound.putInt("chest_max_damage", state.chestMaxDamage());
         compound.putBoolean("glider_has_enchantments", state.gliderHasEnchantments());
         compound.putBoolean("chest_original_swappable", state.chestOriginalSwappable());
         compound.put("glider", gliderSnapshot);
         tag.put("universal_elytra_chestplate", compound);
      });
   }

   private static void putIfPresent(CompoundTag compound, String key, Tag value) {
      if (value != null) {
         compound.put(key, value);
      }
   }

   public static void updateChestSwappable(ItemStack stack, boolean swappable) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
         if (tag.get("universal_elytra_chestplate") instanceof CompoundTag compound) {
            compound.putBoolean("chest_original_swappable", swappable);
         }
      });
   }

   public static void updateChestMaxDamage(ItemStack stack, int chestMaxDamage) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
         if (tag.get("universal_elytra_chestplate") instanceof CompoundTag compound) {
            compound.putInt("chest_max_damage", Math.max(0, chestMaxDamage));
         }
      });
   }

   public static void updateGliderDamage(ItemStack stack, int gliderDamage) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
         if (tag.get("universal_elytra_chestplate") instanceof CompoundTag compound) {
            compound.putInt("glider_damage", gliderDamage);
         }
      });
   }

   public static void clear(ItemStack stack) {
      CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove("universal_elytra_chestplate"));
   }

   public static Optional<ItemStack> readSnapshot(ItemStack stack, Provider provider) {
      CustomData custom = (CustomData)stack.get(DataComponents.CUSTOM_DATA);
      if (custom != null && !custom.isEmpty() && custom.copyTag().get("universal_elytra_chestplate") instanceof CompoundTag state) {
         Tag snapshot = state.get("glider");
         return snapshot == null ? Optional.empty() : ItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), snapshot).result();
      } else {
         return Optional.empty();
      }
   }

   public static Optional<Tag> encodeSnapshot(ItemStack snapshot, Provider provider) {
      return ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), snapshot).result();
   }

   public static Tag encodeName(Component name) {
      return name == null ? null : (Tag)ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, name).result().orElse(null);
   }

   public static Optional<Component> decodeName(Tag name) {
      return name == null ? Optional.empty() : ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, name).result();
   }
}
