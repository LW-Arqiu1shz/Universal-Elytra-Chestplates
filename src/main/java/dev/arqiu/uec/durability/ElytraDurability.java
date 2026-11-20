package dev.arqiu.uec.durability;

import dev.arqiu.uec.component.ElytraChestplateState;
import dev.arqiu.uec.equipment.ElytraChestplate;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 双重耐久的损耗归属（仅服务端：两处调用点均有原版 isClientSide 防护）。
 * <p>合并胸甲只有一条原版耐久条（DAMAGE 组件，共享池），本类负责在每次原版
 * {@code hurtAndBreak} 前后把损伤量限制并记入内部拆分：
 * 飞行损耗只计入鞘翅部分（鞘翅部分用尽后不再掉耐久，由禁飞判定负责停止飞行，
 * 绝不侵蚀胸甲部分）；受击损耗优先扣胸甲部分、耗尽后溢出到鞘翅部分。
 * Unbreaking（耐久）、创造模式免损、损毁行为全部仍由原版
 * {@link ItemStack#hurtAndBreak} 完成——合并时鞘翅与胸甲的附魔已经并入了
 * 同一个 {@code ENCHANTMENTS} 组件，因此附魔按原版规则对共享池生效。
 * 唯一的定制点：飞行磨损的附魔判定按鞘翅展示堆做（{@link FlightWearContext}），
 * 使鞘翅附魔对飞行磨损的保护与合并前的独立鞘翅完全一致，不受胸甲分支
 * （原版按被判定物品是否在 {@code enchantable/armor} 标签内分档）削弱。
 * 记账更新写回 {@code minecraft:custom_data}（不注册任何自定义组件类型）。</p>
 */
public final class ElytraDurability {
    private ElytraDurability() {
    }

    /**
     * 代替 {@link ItemStack#hurtAndBreak(int, LivingEntity, EquipmentSlot)} 的入口。
     *
     * @param gliderFirst true = 飞行损耗（只扣鞘翅部分），false = 受击损耗（先扣胸甲部分）
     */
    public static void damage(ItemStack stack, LivingEntity entity, EquipmentSlot slot, int amount, boolean gliderFirst) {
        ElytraChestplateState state = ElytraChestplateState.read(stack).orElse(null);
        if (state == null) {
            // 普通物品（原版鞘翅、普通胸甲）：完全走原版逻辑。
            stack.hurtAndBreak(amount, entity, slot);
            return;
        }
        if (gliderFirst) {
            // 飞行损耗封顶在鞘翅部分的剩余耐久：绝不溢出到胸甲部分。
            int gliderRoom = state.gliderMaxDamage() - ElytraChestplate.split(stack.getDamageValue(), state)[1];
            if (gliderRoom <= 0) {
                return; // 鞘翅部分已耗尽（正常情况此时已被禁飞，防御性兜底）。
            }
            amount = Math.min(amount, gliderRoom);
        }
        int before = stack.getDamageValue();
        // 飞行损耗：附魔判定改用鞘翅完整快照；受击损耗仍按胸甲自身语义走原版。
        ItemStack proxy = gliderFirst
                ? ElytraChestplateState.readSnapshot(stack, entity.level().registryAccess()).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
        if (proxy.isEmpty()) {
            stack.hurtAndBreak(amount, entity, slot);
        } else {
            FlightWearContext.begin(stack, proxy);
            try {
                stack.hurtAndBreak(amount, entity, slot);
            } finally {
                FlightWearContext.end();
            }
        }
        if (stack.isEmpty() || ElytraChestplateState.read(stack).isEmpty()) {
            // 已损毁：整个共享耐久池用尽，物品消失，无需记账。
            return;
        }
        int applied = stack.getDamageValue() - before;
        if (applied <= 0) {
            return; // Unbreaking 生效或创造模式：本次无实际损耗。
        }
        int[] split = ElytraChestplate.split(before, state);
        if (gliderFirst) {
            split[1] += applied; // 上方已封顶，必然不超过鞘翅部分上限。
        } else {
            // 胸甲部分上限 = 合并堆总上限 − 鞘翅上限（记账不保存胸甲的物品与上限）。
            int chestMax = Math.max(0, stack.getMaxDamage() - state.gliderMaxDamage());
            int toChest = Math.min(applied, Math.max(0, chestMax - split[0]));
            split[0] += toChest;
            split[1] += applied - toChest;
        }
        ElytraChestplateState.updateGliderDamage(stack, split[1]);
    }
}
