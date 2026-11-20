package dev.arqiu.uec.mixin.client;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 鞘翅胸甲的物品图标（背包 / 手持 / 展示等全部上下文）：胸甲与鞘翅双层叠加。
 * <p>思路来自参考模组 armored-elytra 的 ArmoredElytraIconMixin：拦截
 * {@code ItemModelResolver#appendItemLayers}，对合并胸甲取消默认的「只有胸甲」叠加，
 * 改为先追加<b>鞘翅快照</b>的图层、再追加<b>胸甲</b>的图层——后追加者绘制在上层，
 * 于是胸甲完整盖在鞘翅之前（鞘翅被部分遮挡）。</p>
 * <p>错位量：胸甲整体<b>上移 10%</b> 图标高度（模型空间 +0.1），鞘翅整体<b>下移 10%</b>
 * （-0.1）。</p>
 * <p>视深分离：共面两层在带深度缓冲的渲染通道里会在重合区 Z-fighting（贴图闪烁），
 * 因此<b>只把鞘翅层向远离观察者一侧后退</b>，胸甲层保持与原版图标同深度（z=0）——
 * 重合部分深度测试必然胸甲胜出（表现为「重合处只保留胸甲的像素」），
 * 胸甲透明处仍可见下层鞘翅；胸甲层不做正向位移，从根上排除被近平面裁切的风险。
 * 退回量按显示场景取值：GUI 烘焙正交投影深度精度低取大值，手持 / 世界场景透视
 * 投影精度足够取小值以免近大远小产生可见缩放差。</p>
 */
@Mixin(ItemModelResolver.class)
public abstract class ElytraChestplateIconMixin {

    /** 胸甲上移 10% 图标高度（图标模型单位高度为 1.0），鞘翅仍下移 10%。 */
    private static final float CHEST_Y_OFFSET = 0.1F;
    private static final float GLIDER_Y_OFFSET = -0.1F;
    /** 鞘翅层视深后移量：背包等 GUI 通道（正交投影、深度精度低）。 */
    private static final float GLIDER_Z_OFFSET_GUI = -0.25F;
    /** 鞘翅层视深后移量：手持 / 掉落 / 展示框等世界通道（透视投影，取小值防缩放差）。 */
    private static final float GLIDER_Z_OFFSET_WORLD = -0.02F;

    /** 重入守卫：内部为胸甲 / 鞘翅快照递归调用 appendItemLayers 时不再展开。 */
    private static final ThreadLocal<Boolean> UEC$EXPANDING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    @Inject(method = "appendItemLayers", at = @At("HEAD"), cancellable = true)
    private void uec$stackElytraUnderChestplate(ItemStackRenderState state, ItemStack stack,
                                                ItemDisplayContext displayContext, Level level,
                                                ItemOwner itemOwner, int seed, CallbackInfo ci) {
        if (UEC$EXPANDING.get() || stack.isEmpty() || !ElytraChestplate.isMerged(stack)) {
            return;
        }
        ItemStack glider = ElytraChestplate.displayGlider(stack);
        if (glider.isEmpty()) {
            return; // 数据异常兜底：按普通胸甲画。
        }
        ci.cancel();
        ItemModelResolver self = (ItemModelResolver) (Object) this;
        UecItemStackRenderStateAccess access = (UecItemStackRenderStateAccess) (Object) state;
        UEC$EXPANDING.set(Boolean.TRUE);
        try {
            // 下层：鞘翅（按记账 id 构造的展示堆，附魔光泽与合并堆一致）。
            int gliderFrom = access.uec$activeLayerCount();
            self.appendItemLayers(state, glider, displayContext, level, itemOwner, seed);
            int gliderTo = access.uec$activeLayerCount();
            // 上层：胸甲（合并堆本体，保留其全部组件表现）。
            self.appendItemLayers(state, stack, displayContext, level, itemOwner, seed);
            int chestTo = access.uec$activeLayerCount();
            float gliderZ = displayContext == ItemDisplayContext.GUI
                    ? GLIDER_Z_OFFSET_GUI : GLIDER_Z_OFFSET_WORLD;
            uec$offsetLayers(access, gliderFrom, gliderTo, GLIDER_Y_OFFSET, gliderZ);
            uec$offsetLayers(access, gliderTo, chestTo, CHEST_Y_OFFSET, 0.0F);
        } finally {
            UEC$EXPANDING.set(Boolean.FALSE);
        }
    }

    /**
     * 对 [from, to) 范围内的图层叠加纵向平移与视深位移。
     * <p>一律经 {@code setLocalTransform}（拷贝写入）合成新矩阵，
     * 不直接改写池化图层持有的 Matrix4f 引用。</p>
     */
    private static void uec$offsetLayers(UecItemStackRenderStateAccess state, int from, int to,
                                         float yOffset, float zOffset) {
        ItemStackRenderState.LayerRenderState[] layers = state.uec$layers();
        for (int i = from; i < to && i < layers.length; i++) {
            ItemStackRenderState.LayerRenderState layer = layers[i];
            if (layer == null) {
                continue;
            }
            Matrix4f current = ((UecLayerRenderStateAccess) layer).uec$localTransform();
            layer.setLocalTransform(new Matrix4f().translation(0.0F, yOffset, zOffset).mul(current));
        }
    }
}
