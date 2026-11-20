package dev.arqiu.uec.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemStackRenderState.class)
public interface UecItemStackRenderStateAccess {
   @Accessor("activeLayerCount")
   int uec$activeLayerCount();

   @Accessor("layers")
   LayerRenderState[] uec$layers();
}
