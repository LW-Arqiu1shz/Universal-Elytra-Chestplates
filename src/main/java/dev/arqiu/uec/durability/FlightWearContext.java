package dev.arqiu.uec.durability;

import net.minecraft.world.item.ItemStack;

/**
 * 飞行磨损的鞘翅语义上下文（服务端，按调用栈生效）。
 * <p>26.3 原版的 {@code minecraft:item_damage} 类附魔效果（耐久保护等）按
 * <b>被判定物品本身</b>分支取强度：位于 {@code #minecraft:enchantable/armor}
 * 标签内的物品（含胸甲）分支的减免远低于标签外物品（含鞘翅）——同为耐久 III，
 * 鞘翅分支减免 75%、胸甲分支只减免 30%。合并胸甲的飞行磨损若直接按自身物品
 * 判定，鞘翅部分的附魔保护会退化成胸甲强度（合并后掉耐速明显变快）。
 * 本上下文在飞行损耗窗口内声明「这次伤害的附魔判定改用鞘翅展示堆做」，
 * 让鞘翅附魔对飞行磨损的保护与合并前完全一致；受击磨损不经过此窗口，
 * 胸甲部分仍按胸甲语义享受附魔。挂载点见 {@code ItemStackFlightWearMixin}。</p>
 */
public final class FlightWearContext {

    /** 窗口内容：{被伤害的真实堆, 供附魔判定的鞘翅展示堆}。 */
    private static final ThreadLocal<ItemStack[]> CURRENT = new ThreadLocal<>();

    private FlightWearContext() {
    }

    /** 开窗口：target 这次伤害的附魔判定改用 proxy。 */
    public static void begin(ItemStack target, ItemStack proxy) {
        CURRENT.set(new ItemStack[]{target, proxy});
    }

    /** 关窗口（必须与 begin 成对，建议 finally）。 */
    public static void end() {
        CURRENT.remove();
    }

    /** 处于飞行损耗窗口内且堆正是窗口目标堆时返回鞘翅展示堆，否则 null（原版语义）。 */
    public static ItemStack proxyFor(ItemStack stack) {
        ItemStack[] pair = CURRENT.get();
        return pair != null && pair[0] == stack ? pair[1] : null;
    }
}
