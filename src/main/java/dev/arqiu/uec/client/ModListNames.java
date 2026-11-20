package dev.arqiu.uec.client;

import dev.arqiu.uec.UniversalElytraChestplates;
import net.minecraft.client.resources.language.I18n;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * NeoForge 26.1.2 的模组列表只显示 neoforge.mods.toml 里写死的 displayName，没有翻译入口。
 * ModList*Mixin 在读取名称处调用这里：只对本模组、且当前语言有 {@value #NAME_KEY} 翻译时换成翻译名。
 * 此类在 Mixin 配置包之外，供 Mixin 安全引用。
 */
public final class ModListNames {
   public static final String NAME_KEY = "fml.menu.mods.info.displayname." + UniversalElytraChestplates.MOD_ID;

   private ModListNames() {
   }

   /** 显示用：翻译名（没有译文时为原名）。 */
   public static String displayName(IModInfo info, String original) {
      if (UniversalElytraChestplates.MOD_ID.equals(info.getModId()) && I18n.exists(NAME_KEY)) {
         return I18n.get(NAME_KEY);
      }
      return original;
   }

   /** 搜索用：翻译名 + 原名，中文界面下搜英文名也能找到。不会显示在界面上。 */
   public static String searchName(IModInfo info, String original) {
      String shown = displayName(info, original);
      return shown.equals(original) ? original : shown + " " + original;
   }
}
