package dev.arqiu.uec.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** ItemStackRenderState 内部层数组的只读访问（供 {@link ElytraChestplateIconMixin} 使用）。 */
@Mixin(ItemStackRenderState.class)
public interface UecItemStackRenderStateAccess {

    @Accessor("activeLayerCount")
    int uec$activeLayerCount();

    @Accessor("layers")
    ItemStackRenderState.LayerRenderState[] uec$layers();
}
