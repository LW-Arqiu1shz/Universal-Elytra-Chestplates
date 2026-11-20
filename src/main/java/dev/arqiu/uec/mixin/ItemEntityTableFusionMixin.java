package dev.arqiu.uec.mixin;

import dev.arqiu.uec.interaction.UecTableFusion;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 锻造台投料融合的触发点：掉落物 tick 尾部（原版逻辑之后，不影响落地、合并、燃烧、拾取）。
 * <p>裁决全部在 {@link UecTableFusion} 内完成，非服务端、非胸甲 / 鞘翅的掉落物会在前两次短路里返回，
 * 因此本注入对普通掉落物几乎零开销。</p>
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityTableFusionMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void uec$fuseOnSmithingTable(CallbackInfo ci) {
        UecTableFusion.onItemTick((ItemEntity) (Object) this);
    }
}
