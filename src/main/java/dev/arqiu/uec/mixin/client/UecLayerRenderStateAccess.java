package dev.arqiu.uec.mixin.client;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** LayerRenderState 局部变换矩阵的访问（供图层错位使用）。 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface UecLayerRenderStateAccess {

    @Accessor("localTransform")
    Matrix4f uec$localTransform();
}
