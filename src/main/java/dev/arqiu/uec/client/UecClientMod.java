package dev.arqiu.uec.client;

import dev.arqiu.uec.UniversalElytraChestplates;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.modlist.DefaultModDisplayInfo;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;

/**
 * 只在客户端加载的入口：模组列表里的名称按当前语言显示（{@value #NAME_KEY}），
 * 描述沿用 NeoForge 默认的 neoforge.screen.mods.info.description.&lt;modid&gt; 翻译键。
 */
@Mod(value = UniversalElytraChestplates.MOD_ID, dist = Dist.CLIENT)
public final class UecClientMod {
   private static final String NAME_KEY = "neoforge.screen.mods.info.displayname." + UniversalElytraChestplates.MOD_ID;

   public UecClientMod(ModContainer container) {
      container.registerExtensionPoint(ModDisplayInfo.class, new DefaultModDisplayInfo(container) {
         @Override
         public Component displayName() {
            return Component.translatableWithFallback(NAME_KEY, container.getModInfo().getDisplayName());
         }
      });
   }
}
