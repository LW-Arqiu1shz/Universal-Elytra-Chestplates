package dev.arqiu.uec.mixin.client;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemModelResolver.class)
public abstract class ElytraChestplateIconMixin {
   private static final float CHEST_Y_OFFSET = 0.1F;
   private static final float GLIDER_Y_OFFSET = -0.1F;
   private static final float GLIDER_Z_OFFSET_GUI = -0.25F;
   private static final float GLIDER_Z_OFFSET_WORLD = -0.02F;
   private static final ThreadLocal<Boolean> UEC$EXPANDING = ThreadLocal.withInitial(() -> Boolean.FALSE);

   // $VF: Could not verify finally blocks. A semaphore variable has been added to preserve control flow.
   // Please report this to the Vineflower issue tracker, at https://github.com/Vineflower/vineflower/issues with a copy of the class file (if you have the rights to distribute it!)
   @Inject(method = "appendItemLayers", at = @At("HEAD"), cancellable = true)
   private void uec$stackElytraUnderChestplate(
      ItemStackRenderState state, ItemStack stack, ItemDisplayContext displayContext, Level level, ItemOwner itemOwner, int seed, CallbackInfo ci
   ) {
      if (!UEC$EXPANDING.get() && !stack.isEmpty() && ElytraChestplate.isMerged(stack)) {
         ItemStack glider = ElytraChestplate.displayGlider(stack);
         if (!glider.isEmpty()) {
            ci.cancel();
            ItemModelResolver self = (ItemModelResolver)(Object)this;
            UecItemStackRenderStateAccess access = (UecItemStackRenderStateAccess)state;
            UEC$EXPANDING.set(Boolean.TRUE);
            boolean var17 = false /* VF: Semaphore variable */;

            try {
               var17 = true;
               int gliderFrom = access.uec$activeLayerCount();
               self.appendItemLayers(state, glider, displayContext, level, itemOwner, seed);
               int gliderTo = access.uec$activeLayerCount();
               self.appendItemLayers(state, stack, displayContext, level, itemOwner, seed);
               int chestTo = access.uec$activeLayerCount();
               float gliderZ = displayContext == ItemDisplayContext.GUI ? -0.25F : -0.02F;
               uec$offsetLayers(access, gliderFrom, gliderTo, -0.1F, gliderZ);
               uec$offsetLayers(access, gliderTo, chestTo, 0.1F, 0.0F);
               var17 = false;
            } finally {
               if (var17) {
                  UEC$EXPANDING.set(Boolean.FALSE);
               }
            }

            UEC$EXPANDING.set(Boolean.FALSE);
         }
      }
   }

   private static void uec$offsetLayers(UecItemStackRenderStateAccess state, int from, int to, float yOffset, float zOffset) {
      LayerRenderState[] layers = state.uec$layers();

      for (int i = from; i < to && i < layers.length; i++) {
         LayerRenderState layer = layers[i];
         if (layer != null) {
            Matrix4f current = ((UecLayerRenderStateAccess)layer).uec$localTransform();
            layer.setLocalTransform(new Matrix4f().translation(0.0F, yOffset, zOffset).mul(current));
         }
      }
   }
}
