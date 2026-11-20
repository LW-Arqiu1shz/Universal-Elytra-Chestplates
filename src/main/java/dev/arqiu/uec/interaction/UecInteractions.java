package dev.arqiu.uec.interaction;

import com.mojang.datafixers.util.Pair;
import dev.arqiu.uec.equipment.ElytraChestplate;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 拆解 / 手持穿戴的触发入口（纯服务端裁决）。<b>合并已改到锻造台台面</b>，
 * 见 {@link UecTableFusion}：把胸甲和鞘翅一起丢到锻造台顶部即融合。
 * <p>选择服务端裁决而不是配方系统的根本原因：任何自定义的配方序列化器条目都会随
 * 注册表网络数据下发，未安装模组的客户端无法解析未知配方类型会直接进服失败；
 * 自定义数据组件同理会破坏 ItemStack 解码。本交互不产生任何注册表条目，
 * 所有物品状态写入都发生在服务端并由原版槽位同步下发，
 * 原版 / NeoForge 客户端零安装即可使用完整的合并、飞行与拆解功能。</p>
 * <p>对客户端无预测：返回 {@code SUCCESS_SERVER}，物品变化全部经由服务端
 * 槽位同步生效，因此不存在「客户端先画了一份、服务端又改回去」的幽灵物品。</p>
 * <p><b>幽灵装备三重防线</b>（26.x 的 {@code Equippable.swappable} 会让客户端把
 * 「右键手持盔甲」本地预测为直接穿进胸甲槽，与拆解/合并的服务端结果分叉后，
 * 盔甲槽又不在容器同步范围内，预测残留变成顶掉胸甲的幽灵图标）：
 * <ol>
 *   <li>合并堆的 EQUIPPABLE 已改写为 {@code swappable=false}（见
 *       {@link ElytraChestplate#merge}），客户端对合并胸甲不再有穿戴预测；</li>
 *   <li>非潜行右键合并胸甲改由服务端手工换装（合并堆穿上身、原穿戴物回手），
 *       右键穿戴体验保留，且不依赖原版 {@code heldItemTransformedTo} 后处理；</li>
 *   <li>每次 拆解 / 代穿戴后向当事玩家补发一条原版装备同步包，
 *       纠正其盔甲槽（含未装模组的客户端对普通鞘翅 / 胸甲的预测残留，
 *       以及本模组修复前产生的旧合并堆）。</li>
 * </ol></p>
 */
public final class UecInteractions {
    private UecInteractions() {
    }

    public static void init() {
        UseItemCallback.EVENT.register(UecInteractions::onUseItem);
    }

    private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.PASS; // 客户端一律交还原版逻辑（合并堆已无穿戴预测，见类注释）。
        }
        ItemStack held = player.getItemInHand(hand);
        if (ElytraChestplate.isMerged(held)) {
            HolderLookup.Provider provider = level.registryAccess();
            // 合并堆的 swappable=false，原版 Item#use 不会再穿戴它——由服务端代执行。
            return player.isSecondaryUseActive()
                    ? trySplit(player, hand, provider)
                    : tryWear(player, hand, held);
        }
        return InteractionResult.PASS; // 其余一律交还原版：合并请去锻造台台面（UecTableFusion）。
    }

    /**
     * 服务端代执行右键穿戴：调用原版 {@code swapWithEquipmentSlot}，
     * 并<b>手动把返回值 {@code heldItemTransformedTo} 里被换下的穿戴物写回手上</b>。
     * <p>原版 {@code Item#use} 的换装之所以不丢东西，是因为
     * {@code ServerPlayerGameMode} 在 {@code stack.use(...)} 调用点之后会把
     * {@code heldItemTransformedTo} 回写到手槽。而本入口对合并堆返回 SUCCESS_SERVER、
     * 跳过了那段后处理：被换下的胸甲只存在于已被丢弃的返回值里，原版换装内部又已把它
     * 从胸甲槽 {@code copyAndClear}——穿戴物凭空消失、客户端盔甲槽残留幽灵。
     * 这里复刻后处理语义（引用相同则不回写，与原版判定一致），任何一方的物品都不再被吞。</p>
     */
    private static InteractionResult tryWear(Player player, InteractionHand hand, ItemStack held) {
        Equippable equippable = held.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return InteractionResult.PASS;
        }
        InteractionResult result = equippable.swapWithEquipmentSlot(held, player);
        if (!(result instanceof InteractionResult.Success success)) {
            return result; // PASS / FAIL 原样上抛，交回原版（或什么都不做），不动任何物品。
        }
        ItemStack transformed = success.heldItemTransformedTo();
        // 与原版后处理一致：仅当被换下的堆不是手持堆本身时才回写手槽
        // （手持堆本身被 copyAndClear 就地清空时，回写反而会把空堆当新引用塞回去）。
        if (transformed != null && transformed != held && !transformed.isEmpty()) {
            player.setItemInHand(hand, transformed);
        }
        player.containerMenu.broadcastChanges();
        resyncChestEquipment(player); // 客户端对 swappable=false 的堆没有本地预测，盔甲槽需服务端纠正下发。
        return InteractionResult.SUCCESS_SERVER;
    }

    /** 拆解：手持合并胸甲 → 胸甲回到手中，鞘翅依次尝试：另一只手 → 背包 → 脚下掉落。 */
    private static InteractionResult trySplit(Player player, InteractionHand hand, HolderLookup.Provider provider) {
        ItemStack merged = player.getItemInHand(hand);
        Optional<ItemStack> chest = ElytraChestplate.restoredChest(merged);
        Optional<ItemStack> glider = ElytraChestplate.restoredGlider(merged, provider);
        if (chest.isEmpty() || glider.isEmpty()) {
            return InteractionResult.PASS; // 数据损坏兜底：保持原物，绝不吞物品。
        }
        player.setItemSlot(hand.asEquipmentSlot(), chest.get());
        InteractionHand otherHand = opposite(hand);
        if (player.getItemInHand(otherHand).isEmpty()) {
            player.setItemSlot(otherHand.asEquipmentSlot(), glider.get());
        } else if (!player.getInventory().add(glider.get())) {
            player.drop(glider.get(), false, Prediction.SERVER_ONLY);
        }
        player.containerMenu.broadcastChanges(); // 同上：即时同步。
        resyncChestEquipment(player);            // 同上：纠正盔甲槽预测残留。
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * 向当事玩家补发一条原版盔甲槽同步包。
     * <p>玩家的盔甲槽内容不随背包容器（菜单未打开时）同步，客户端右键穿戴是纯本地预测；
     * 只要服务端没有真的穿戴，就必须主动纠正，否则预测残留会表现为
     * 「幽灵鞘翅顶掉胸甲」直到下次打开背包。对未装模组的原版客户端同样有效。</p>
     */
    public static void resyncChestEquipment(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.connection.send(new ClientboundSetEquipmentPacket(
                    serverPlayer.getId(),
                    List.of(Pair.of(EquipmentSlot.CHEST, serverPlayer.getItemBySlot(EquipmentSlot.CHEST)))));
        }
    }

    private static InteractionHand opposite(InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }
}
