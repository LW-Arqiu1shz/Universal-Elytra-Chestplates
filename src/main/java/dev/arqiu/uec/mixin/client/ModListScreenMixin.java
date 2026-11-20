package dev.arqiu.uec.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arqiu.uec.client.ModListNames;
import net.neoforged.neoforge.client.gui.ModListScreen;
import net.neoforged.neoforgespi.language.IModInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 模组列表界面：右侧信息面板标题、列表宽度用翻译名；搜索同时匹配翻译名与原名。 */
@Mixin(ModListScreen.class)
public abstract class ModListScreenMixin {
   @WrapOperation(method = {"init", "updateCache"},
         at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
   private String uec$translatedName(IModInfo info, Operation<String> original) {
      return ModListNames.displayName(info, original.call(info));
   }

   /** reloadMods 里的搜索过滤 lambda。require = 0：别的 NeoForge 小版本编号可能不同，挂不上只少了双名搜索，不会崩。 */
   @WrapOperation(method = "lambda$reloadMods$0", require = 0,
         at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforgespi/language/IModInfo;getDisplayName()Ljava/lang/String;"))
   private String uec$searchBothNames(IModInfo info, Operation<String> original) {
      return ModListNames.searchName(info, original.call(info));
   }
}
