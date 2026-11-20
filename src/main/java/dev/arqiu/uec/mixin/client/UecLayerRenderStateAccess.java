package dev.arqiu.uec.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LayerRenderState.class)
public interface UecLayerRenderStateAccess {
   @Accessor("localTransform")
   Matrix4f uec$localTransform();
}
