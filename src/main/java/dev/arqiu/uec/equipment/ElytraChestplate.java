package dev.arqiu.uec.equipment;

import dev.arqiu.uec.component.ElytraChestplateState;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.Optional;

/**
 * 合并 / 拆解的核心物品逻辑（全部在服务端执行；合并由锻造台台面投料触发，
 * 见 {@link dev.arqiu.uec.interaction.UecTableFusion}，拆解由手持交互触发）。
 * <p>判定「胸甲」一律通过 {@code EQUIPPABLE} 组件的装备槽位，不使用固定物品白名单，
 * 因此天然兼容其他模组添加的胸甲；判定「鞘翅」通过原版 {@code GLIDER} 组件。</p>
 * <p>状态存放于原版 {@code minecraft:custom_data} 组件（见 {@link ElytraChestplateState}），
 * 合并堆的全部组件类型均为原版类型：未安装本模组的客户端可以正常收发包并忽略模组数据，
 * 服务端单独安装即可对整个服务器生效。</p>
 */
public final class ElytraChestplate {
    private ElytraChestplate() {
    }

    /** 该堆是否为已安装鞘翅的合并胸甲。 */
    public static boolean isMerged(ItemStack stack) {
        return ElytraChestplateState.has(stack);
    }

    /** 物品类型是否可装备在胸部槽位（按 EQUIPPABLE 组件判定，兼容模组装具）。 */
    public static boolean isChestArmorItem(Item item) {
        Equippable equippable = item.components().get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    /** 是否可作为合并底材：胸甲、未安装鞘翅、且本身不是滑翔装备（防止鞘翅+鞘翅合并）。 */
    public static boolean canMergeInto(ItemStack stack) {
        return !stack.isEmpty() && isChestArmorItem(stack.getItem()) && !isMerged(stack)
                && !stack.has(DataComponents.GLIDER);
    }

    /** 是否可作为合并材料：带原版 GLIDER 组件、可穿戴于胸部槽位、自身不是合并胸甲。 */
    public static boolean isGlider(ItemStack stack) {
        if (stack.isEmpty() || isMerged(stack) || !stack.has(DataComponents.GLIDER)) {
            return false;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    /**
     * 把鞘翅装到胸甲上（服务端）：保留胸甲的全部组件（附魔、纹饰、染色、名称等），
     * 追加 GLIDER 与记账数据，耐久合并为「胸甲损伤 + 鞘翅损伤」/「胸甲上限 + 鞘翅上限」。
     * <p>记账只记鞘翅部分（见 {@link ElytraChestplateState}）与胸甲自己的名称，
     * 胸甲物品本身不落账：拆解时胸甲取当前物品，升级 / 附魔后依然正确。</p>
     * <p>胸甲与鞘翅的附魔严格隔离：合并堆的 ENCHANTMENTS 只保留胸甲附魔；鞘翅附魔保存在完整快照中，飞行耐久计算读取快照，客户端仅依据快照标记显示光泽。</p>
     *
     * @return 合并结果；快照编码失败时返回 empty（调用方应放弃合并）
     */
    public static Optional<ItemStack> merge(ItemStack chest, ItemStack glider, HolderLookup.Provider provider) {
        int gliderDamage = glider.getDamageValue();
        int gliderMax = glider.getMaxDamage();

        Optional<Tag> gliderSnapshot = ElytraChestplateState.encodeSnapshot(snapshot(glider), provider);
        if (gliderSnapshot.isEmpty()) {
            return Optional.empty(); // 防御：无法编码快照则不合并，避免产生不可拆解的物品。
        }

        ItemStack merged = chest.copyWithCount(1);
        merged.set(DataComponents.MAX_DAMAGE, chest.getMaxDamage() + gliderMax);
        merged.setDamageValue(chest.getDamageValue() + gliderDamage);
        merged.set(DataComponents.GLIDER, Unit.INSTANCE);
        merged.set(DataComponents.CUSTOM_NAME, combinedName(
                chest.getHoverName(), glider.getHoverName(),
                chest.has(DataComponents.CUSTOM_NAME) || glider.has(DataComponents.CUSTOM_NAME)));
        // 保留胸甲原有 EQUIPPABLE 的全部属性，仅确保合并胸甲仍可走原版快速装备路径。
        Equippable equippable = merged.get(DataComponents.EQUIPPABLE);
        if (equippable != null && !equippable.swappable()) {
            merged.set(DataComponents.EQUIPPABLE, withSwappable(equippable, true));
        }
        ElytraChestplateState.write(merged,
                new ElytraChestplateState(id(glider.getItem()), gliderDamage, gliderMax,
                        ElytraChestplateState.encodeName(unstyled(glider.getHoverName())),
                        ElytraChestplateState.encodeName(chest.get(DataComponents.CUSTOM_NAME)),
                        chest.getMaxDamage(),
                        !glider.getOrDefault(DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY).isEmpty(),
                        (chest.get(DataComponents.EQUIPPABLE) != null && chest.get(DataComponents.EQUIPPABLE).swappable())),
                gliderSnapshot.get());
        return Optional.of(merged);
    }

    /**
     * 升级类操作（锻造台把钻石胸甲换下界合金胸甲等）会把合并堆的全部组件原样搬到新物品上，
     * 其中「随胸甲物品而变」的组件会停留在旧胸甲上：耐久上限、装备属性（盔甲模型资产）、显示名称。
     * 这里把它们拉回当前物品；不带记账的普通物品一律不动。
     */
    public static void applyCurrentItem(ItemStack merged) {
        ElytraChestplateState state = ElytraChestplateState.read(merged).orElse(null);
        if (state == null) {
            return;
        }
        Integer itemMax = merged.getItem().components().get(DataComponents.MAX_DAMAGE);
        int currentChestMax = itemMax == null ? 0 : Math.max(0, itemMax);
        if (currentChestMax + state.gliderMaxDamage() > 0) {
            merged.set(DataComponents.MAX_DAMAGE, currentChestMax + state.gliderMaxDamage());
        } else {
            merged.remove(DataComponents.MAX_DAMAGE);
        }
        ElytraChestplateState.updateChestMaxDamage(merged, currentChestMax);
        Equippable prototype = merged.getItem().components().get(DataComponents.EQUIPPABLE);
        if (prototype != null) {
            merged.set(DataComponents.EQUIPPABLE, withSwappable(prototype, true));
            ElytraChestplateState.updateChestSwappable(merged, prototype.swappable());
        }
        Optional<Component> gliderName = ElytraChestplateState.decodeName(state.gliderName());
        if (gliderName.isPresent()) {
            Component current = merged.get(DataComponents.CUSTOM_NAME);
            boolean italic = current != null && current.getStyle().isItalic();
            Component chestName = ElytraChestplateState.decodeName(state.chestName())
                    .orElseGet(() -> Component.translatable(merged.getItem().getDescriptionId()));
            merged.set(DataComponents.CUSTOM_NAME,
                    combinedName(chestName, gliderName.get(), italic));
        }
    }

    /**
     * 拆解主输出：<b>当前胸甲</b>——物品类型、附魔、组件全部取自当前合并堆
     * （因此升级成下界合金或再次附魔后，拆出来的就是升级后的胸甲），
     * 只剥掉鞘翅相关部分：{@code GLIDER} 组件、合并时关闭右键直穿的 {@code EQUIPPABLE}
     * （保留胸甲原有完整组件）、合并时叠加的耐久上限（从合并账本还原胸甲自己的上限），名称回到胸甲自己的独立自定义名称
     * （丢掉合并时生成的组合名称），损伤扣除鞘翅部分。
     * <p>因此合并前后胸甲的「剩余耐久 = 自己的上限 − 自己那一份损伤」保持不变：
     * 胸甲上限被其他模组 / 数据包改写（例如钻石胸甲只有 200 点上限）时不会退回物品默认值，
     * 也不会因此凭空回满耐久。</p>
     */
    public static Optional<ItemStack> restoredChest(ItemStack merged) {
        Optional<ElytraChestplateState> stateOpt = ElytraChestplateState.read(merged);
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }
        ElytraChestplateState state = stateOpt.get();
        ItemStack chest = merged.copyWithCount(1);
        chest.remove(DataComponents.GLIDER);
        int chestMax = state.chestMaxDamage() >= 0
                ? state.chestMaxDamage() : legacyChestMaxDamage(merged, state);
        if (chestMax > 0) {
            chest.set(DataComponents.MAX_DAMAGE, chestMax);
        } else {
            chest.remove(DataComponents.MAX_DAMAGE);
        }
        // 不删除 EQUIPPABLE：合并过程保留了胸甲当前的完整装备组件，避免丢失模组自定义属性。
        Equippable restoredEquippable = chest.get(DataComponents.EQUIPPABLE);
        if (restoredEquippable != null) {
            chest.set(DataComponents.EQUIPPABLE, withSwappable(restoredEquippable, state.chestOriginalSwappable()));
        }
        ElytraChestplateState.clear(chest);
        ElytraChestplateState.decodeName(state.chestName())
                .ifPresentOrElse(name -> chest.set(DataComponents.CUSTOM_NAME, name),
                        () -> chest.remove(DataComponents.CUSTOM_NAME));
        // 顺序要紧：setDamageValue 会按当前上限裁剪，必须先定上限再写损伤。
        chest.setDamageValue(split(merged.getDamageValue(), state)[0]);
        return Optional.of(chest);
    }

    /**
     * 胸甲部分自身的耐久上限 = 合并堆上限 − 记账里的鞘翅上限
     * （账本不存胸甲物品与胸甲上限，故只能这样反推；锻造升级后 {@link #applyCurrentItem}
     * 已把合并堆上限重算成「新材料上限 + 鞘翅上限」，反推结果自然就是新材料上限）。
     *
     * @return 需要显式写回 {@code MAX_DAMAGE} 的胸甲上限；
     *         返回 {@code -1} 表示与当前物品的默认值一致（或账目不含可还原的叠加值），
     *         拆解时应删掉该组件、跟随物品默认值
     */

    /** 兼容旧版合并物品：旧账本没有保存胸甲耐久上限，只能按旧规则反推。 */
    private static int legacyChestMaxDamage(ItemStack merged, ElytraChestplateState state) {
        Integer prototype = merged.getItem().components().get(DataComponents.MAX_DAMAGE);
        if (prototype == null || prototype <= 0) {
            return 0;
        }
        int inferred = merged.getMaxDamage() - Math.max(0, state.gliderMaxDamage());
        return inferred > 0 ? inferred : prototype;
    }

    /** 拆解额外返还：原鞘翅（快照还原：含附魔、纹饰、原名称与该部分耐久）。 */
    public static Optional<ItemStack> restoredGlider(ItemStack merged, HolderLookup.Provider provider) {
        Optional<ElytraChestplateState> stateOpt = ElytraChestplateState.read(merged);
        if (stateOpt.isEmpty()) {
            return Optional.empty();
        }
        ElytraChestplateState state = stateOpt.get();
        // 优先用完整快照还原；快照损坏时退化为按 id 重建裸物品（至少不吞物品）。
        Optional<ItemStack> base = ElytraChestplateState.readSnapshot(merged, provider)
                .or(() -> {
                    Item item = itemById(state.gliderItem());
                    return item == null ? Optional.empty() : Optional.of(new ItemStack(item));
                });
        return base.map(part -> {
            ItemStack glider = part.copyWithCount(1);
            glider.set(DataComponents.MAX_DAMAGE, state.gliderMaxDamage());
            glider.setDamageValue(split(merged.getDamageValue(), state)[1]);
            return glider;
        });
    }

    /**
     * 客户端安全的展示堆：按记账里的鞘翅 id 构造一个原版鞘翅堆
     * （图标叠层与背部翅膀渲染用），并仅按鞘翅自身的附魔标记显示光泽。
     * 实际附魔数据由快照独立保存，飞行耐久计算读取快照。物品 id 无效时返回 {@link ItemStack#EMPTY}（调用方回退普通胸甲表现）。
     */
    public static ItemStack displayGlider(ItemStack merged) {
        ElytraChestplateState state = ElytraChestplateState.read(merged).orElse(null);
        if (state == null) {
            return ItemStack.EMPTY;
        }
        Item gliderItem = itemById(state.gliderItem());
        if (gliderItem == null) {
            return ItemStack.EMPTY;
        }
        ItemStack display = new ItemStack(gliderItem);
        // 这里只显示鞘翅自己的附魔光泽；不复制胸甲 ENCHANTMENTS，避免视觉串号。
        if (state.gliderHasEnchantments()) {
            display.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return display;
    }

    /**
     * 按记账把合并堆的当前总损伤拆成（胸甲部分，鞘翅部分）。
     * 鞘翅部分按记账值（封顶在自身上限），其余全部归胸甲——因此外部修复
     * （经验修补、铁砧等）带来的耐久变化先落在胸甲部分，两者相加恒等于总损伤，
     * 反复合并 / 拆解不会凭空恢复耐久。
     */
    public static int[] split(int totalDamage, ElytraChestplateState state) {
        int total = Math.max(totalDamage, 0);
        int gliderPart = Mth.clamp(state.gliderDamage(), 0, state.gliderMaxDamage());
        if (gliderPart > total) {
            gliderPart = total;
        }
        return new int[]{total - gliderPart, gliderPart};
    }

    /** 快照：复制物品堆并剥离耐久组件（耐久统一记账在状态字段里）。 */
    private static ItemStack snapshot(ItemStack stack) {
        ItemStack copy = stack.copyWithCount(1);
        copy.remove(DataComponents.DAMAGE);
        copy.remove(DataComponents.MAX_DAMAGE);
        return copy;
    }

    private static String id(Item item) {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return key == null ? "" : key.toString();
    }

    private static Item itemById(String id) {
        try {
            return BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * 合并名称规则（可本地化）：客户端按自身语言渲染——
     * 中文「加装（鞘翅名）的（胸甲名）」（例：加装鞘翅的下界合金胸甲），
     * 英文「（胸甲名） with （鞘翅名）」（例：Netherite Chestplate with Elytra）。
     * <p>名字存为带参数的 translatable 组件（原版组件类型，纯数据）：
     * 装了本模组（或加载了资源包）的客户端用本模组 lang 翻译；
     * 未安装的原版客户端走 {@code translatableWithFallback} 的英文模板兜底，
     * 胸甲 / 鞘翅两段名字本身仍是原版 / 模组物品名组件，按客户端语言各自渲染。</p>
     * <p>各段一律剥离自带样式（颜色、斜体等），颜色统一交给外层按胸甲稀有度渲染。
     * 斜体按原版自定义名称规则：只有任意一方被玩家重命名过才是斜体，
     * 否则显式 {@code italic:false} 关闭（原版对 item_name 默认渲染斜体）。
     * 拆解时按记账恢复胸甲自己的名称。</p>
     */
    private static Component combinedName(Component chestName, Component gliderName, boolean renamed) {
        MutableComponent name = Component.translatableWithFallback(
                "item.universal_elytra_chestplates.name.merged",
                "%1$s with %2$s",
                unstyled(chestName), unstyled(gliderName));
        // 显式指定斜体状态：未重命名 → 非斜体（覆盖原版对自定义名称的斜体默认）。
        name.setStyle(Style.EMPTY.withItalic(renamed));
        return name;
    }

    /** 复制装备属性，只改写快速装备开关。 */
    private static Equippable withSwappable(Equippable source, boolean swappable) {
        return new Equippable(source.slot(), source.equipSound(), source.assetId(), source.cameraOverlay(),
                source.allowedEntities(), source.dispensable(), swappable, source.damageOnHurt(),
                source.equipOnInteract(), source.canBeSheared(), source.shearingSound());
    }

    /** 复制组件并清空其样式，保留可本地化的内容（翻译键、文本、子组件）。 */
    private static Component unstyled(Component part) {
        MutableComponent copy = part.copy();
        copy.setStyle(Style.EMPTY);
        return copy;
    }
}
