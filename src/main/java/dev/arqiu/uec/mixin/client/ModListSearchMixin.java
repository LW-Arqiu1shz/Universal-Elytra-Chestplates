package dev.arqiu.uec.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.arqiu.uec.client.ModListSearch;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.gui.modlist.ModDisplayInfo;
import net.neoforged.neoforge.client.gui.modlist.ModListScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** 模组列表搜索过滤（updateModsList 里的 lambda）：本模组同时按翻译名与原名匹配。 */
@Mixin(ModListScreen.class)
public abstract class ModListSearchMixin {
   /** require = 0：别的 NeoForge 小版本 lambda 编号可能不同，挂不上只少了双名搜索，不会崩。 */
   @WrapOperation(method = "lambda$updateModsList$0", require = 0,
         at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/client/gui/modlist/ModDisplayInfo;displayName()Lnet/minecraft/network/chat/Component;"))
   private static Component uec$searchBothNames(ModDisplayInfo info, Operation<Component> original) {
      return ModListSearch.searchName(info.id(), original.call(info));
   }
}
