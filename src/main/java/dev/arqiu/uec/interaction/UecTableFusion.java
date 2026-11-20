package dev.arqiu.uec.interaction;

import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * 锻造台投料融合：把一件胸甲和一件鞘翅<b>一起丢到锻造台顶部</b>，两者当场敲合成一件鞘翅胸甲，
 * 同时发出一声清脆的金属泛音并迸出少量闪光粒子，成品留在台面正中可以立刻捡起。
 * <p>与原来的手持交互相比，这条路径<b>不碰任何容器与盔甲槽</b>：全部变化都发生在掉落物上，
 * 由服务端销毁两件原料并 {@code addFreshEntity} 生成成品，原版协议自带掉落物增删同步，
 * 因此不存在「客户端先画了一份、服务端又改回去」的幽灵物品，未装模组的客户端同样正常生效。</p>
 * <p>触发点为 {@code ItemEntity#tick} 的服务端尾部（见
 * {@link dev.arqiu.uec.mixin.ItemEntityTableFusionMixin}）。判定顺序按开销从小到大短路，
 * 绝大多数掉落物只付出一次组件查询的代价：</p>
 * <ol>
 *   <li>手持交互里同样的两件判定：可作底材的胸甲 / 可作材料的鞘翅（都不含已合并堆）；</li>
 *   <li>所在方块或其正下方方块是锻造台；</li>
 *   <li>同一台锻造台的台面范围内存在<b>互补</b>的另一件掉落物。</li>
 * </ol>
 * <p>两件物品都要求数量为 1（胸甲与鞘翅本身不可堆叠，此处只是防住可堆叠的模组甲具被复制），
 * 且成品本身带合并记账，{@code canMergeInto} 与 {@code isGlider} 都会把它判为否，
 * 因此融合结果继续留在台面上不会二次融合。</p>
 */
public final class UecTableFusion {
    /** 成品的拾取冷却（tick）：留出一瞬让玩家听见铃声、看见粒子，而不是立刻被吸回背包。 */
    private static final int RESULT_PICKUP_DELAY = 10;
    /** 成品轻微上弹，配合粒子更像「叮」地敲合上去。 */
    private static final double RESULT_POP = 0.08;

    /** 台面搜索范围：方块本体外扩一点，纵向多带一格，接住刚落台与尚未落稳的另一件。 */
    private static final double SEARCH_INFLATE_XZ = 0.35;
    private static final double SEARCH_INFLATE_UP = 1.2;

    /** 铃声参数（音源为原版紫水晶泛音，清脆且无需注册任何音频条目）。 */
    private static final float CHIME_VOLUME = 1.0F;
    private static final float CHIME_PITCH = 1.15F;

    /** 粒子参数：END_ROD 少量迸发。 */
    private static final int PARTICLE_COUNT = 14;
    private static final double PARTICLE_SPREAD_XZ = 0.25;
    private static final double PARTICLE_SPREAD_Y = 0.12;
    private static final double PARTICLE_SPEED = 0.01;

    private UecTableFusion() {
    }

    /**
     * 掉落物服务端 tick 尾部入口。
     * <p>只裁决、不预测：任何客户端（含原版客户端）都只通过原版掉落物同步包看到结果。</p>
     */
    public static void onItemTick(ItemEntity self) {
        if (self.isRemoved()) {
            return; // 同一 tick 内已被另一件物品的融合消耗掉。
        }
        Level level = self.level();
        if (!(level instanceof ServerLevel serverLevel)) {
            return; // 纯服务端裁决。
        }
        ItemStack stack = self.getItem();
        if (stack.getCount() != 1) {
            return;
        }
        boolean asChest = ElytraChestplate.canMergeInto(stack);
        boolean asGlider = !asChest && ElytraChestplate.isGlider(stack);
        if (!asChest && !asGlider) {
            return;
        }
        BlockPos table = tableUnder(self);
        if (table == null) {
            return; // 不在锻造台台面上。
        }
        ItemEntity partner = findPartner(self, serverLevel, table, asChest);
        if (partner == null) {
            return;
        }
        ItemStack chest = asChest ? stack : partner.getItem();
        ItemStack glider = asChest ? partner.getItem() : stack;
        ElytraChestplate.merge(chest, glider, serverLevel.registryAccess())
                .ifPresent(merged -> fuse(serverLevel, self, partner, merged, table));
    }

    /** 融合落账：销毁两件原料，在台面正中生成成品，并给出声音 + 粒子反馈。 */
    private static void fuse(ServerLevel level, ItemEntity chest, ItemEntity glider, ItemStack merged, BlockPos table) {
        double x = table.getX() + 0.5;
        double z = table.getZ() + 0.5;
        double top = table.getY() + 1.0;
        chest.discard();
        glider.discard();
        ItemEntity result = new ItemEntity(level, x, top + 0.05, z, merged);
        result.setDeltaMovement(0.0, RESULT_POP, 0.0);
        result.setPickUpDelay(RESULT_PICKUP_DELAY);
        level.addFreshEntity(result);
        level.playSound(null, x, top, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, CHIME_VOLUME, CHIME_PITCH);
        level.sendParticles(ParticleTypes.END_ROD, x, top + 0.15, z,
                PARTICLE_COUNT, PARTICLE_SPREAD_XZ, PARTICLE_SPREAD_Y, PARTICLE_SPREAD_XZ, PARTICLE_SPEED);
    }

    /** 该掉落物所在或正下方的锻造台；都不是则返回 {@code null}。 */
    private static BlockPos tableUnder(ItemEntity item) {
        Level level = item.level();
        BlockPos at = item.blockPosition();
        if (isTable(level, at)) {
            return at;
        }
        BlockPos below = at.below();
        return isTable(level, below) ? below : null;
    }

    private static boolean isTable(Level level, BlockPos pos) {
        return level.getBlockState(pos).is(Blocks.SMITHING_TABLE);
    }

    /** 同一台锻造台的台面上，与本件互补的另一件掉落物（合并后的成品不在此列）。 */
    private static ItemEntity findPartner(ItemEntity self, ServerLevel level, BlockPos table, boolean selfIsChest) {
        for (ItemEntity other : level.getEntitiesOfClass(ItemEntity.class, topRegion(table),
                other -> other != self && restsOn(other, table) && isPartner(other.getItem(), selfIsChest))) {
            return other;
        }
        return null;
    }

    private static boolean isPartner(ItemStack stack, boolean selfIsChest) {
        if (stack.getCount() != 1) {
            return false;
        }
        return selfIsChest ? ElytraChestplate.isGlider(stack) : ElytraChestplate.canMergeInto(stack);
    }

    /** 掉落物是否贴着这张锻造台的台面（避免把地面上紧邻锻造台的另一件算进来）。 */
    private static boolean restsOn(ItemEntity item, BlockPos table) {
        BlockPos at = item.blockPosition();
        return at.equals(table) || at.below().equals(table);
    }

    private static AABB topRegion(BlockPos table) {
        return new AABB(table).inflate(SEARCH_INFLATE_XZ, SEARCH_INFLATE_UP, SEARCH_INFLATE_XZ);
    }
}
