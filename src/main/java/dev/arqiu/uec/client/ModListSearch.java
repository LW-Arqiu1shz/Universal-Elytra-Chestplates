package dev.arqiu.uec.client;

import dev.arqiu.uec.UniversalElytraChestplates;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;

/**
 * NeoForge 26.3 模组列表的搜索只匹配 displayName()；名称翻译后，中文界面下搜英文名就搜不到了。
 * ModListSearchMixin 在搜索过滤处调用这里，把匹配文本换成「翻译名 + 原名」，显示的名称不变。
 * 此类在 Mixin 配置包之外，供 Mixin 安全引用。
 */
public final class ModListSearch {
   private ModListSearch() {
   }

   public static Component searchName(String modId, Component shown) {
      if (!UniversalElytraChestplates.MOD_ID.equals(modId)) {
         return shown;
      }
      String original = ModList.get().getModContainerById(modId)
            .map(container -> container.getModInfo().getDisplayName())
            .orElse("");
      String translated = shown.getString();
      return original.isEmpty() || translated.equals(original) ? shown : Component.literal(translated + " " + original);
   }
}
