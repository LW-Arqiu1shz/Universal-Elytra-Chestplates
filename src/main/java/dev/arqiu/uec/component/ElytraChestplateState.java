package dev.arqiu.uec.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * 合并胸甲的记账状态：<b>只记鞘翅部分的数据</b>（物品 id、耐久、完整快照）
 * 与胸甲自己的名称；胸甲的物品 id / 快照 / 耐久一律不落账，
 * 拆解时胸甲一律取当前物品，因此把合并胸甲升级（如钻石→下界合金）或再附魔后，
 * 拆解还原的仍是升级后的胸甲。
 * <p>字段全部是原版标量 / NBT，存放在原版 {@code minecraft:custom_data} 组件内
 * 本模组专属键（{@link #KEY}）之下：模组不注册任何自定义数据组件类型，
 * ItemStack 网络包中出现的组件类型全部是原版类型——未安装本模组（乃至未装 Fabric）
 * 的客户端都能正常解码并原样忽略这段 NBT，服务端单独安装即可生效。</p>
 * <p>鞘翅的完整快照以原版 {@code ItemStack.CODEC} 编码为 NBT 子标签，只有服务端解析
 * （拆解还原用）；客户端各渲染 / 提示路径只读标量字段。</p>
 *
 * @param gliderItem      鞘翅部分物品 id（客户端构造展示堆用）
 * @param gliderDamage    鞘翅部分当前损伤
 * @param gliderMaxDamage 鞘翅部分最大耐久
 * @param gliderName      鞘翅部分名称（合并显示名 / 升级后重建显示名用，可为 {@code null}）
 * @param chestName       胸甲合并前的独立自定义名称（可为 {@code null}，即胸甲本来没有自定义名称）
 * @param chestMaxDamage  胸甲合并前的实际最大耐久（0 表示无耐久）
 * @param gliderHasEnchantments 鞘翅是否有附魔，仅供客户端显示光泽；实际附魔来自完整快照
 * @param chestOriginalSwappable 胸甲原本的 EQUIPPABLE.swappable 值
 */
public record ElytraChestplateState(
        String gliderItem,
        int gliderDamage,
        int gliderMaxDamage,
        Tag gliderName,
        Tag chestName,
        int chestMaxDamage,
        boolean gliderHasEnchantments,
        boolean chestOriginalSwappable) {

    /** custom_data 内本模组状态compound的键名。 */
    public static final String KEY = "universal_elytra_chestplate";
    /** 状态compound内鞘翅快照的键名（仅服务端解析）。 */
    public static final String GLIDER_SNAPSHOT = "glider";

    private static final String F_GLIDER_ITEM = "glider_item";
    private static final String F_GLIDER_DAMAGE = "glider_damage";
    private static final String F_GLIDER_MAX = "glider_max_damage";
    private static final String F_GLIDER_NAME = "glider_name";
    private static final String F_CHEST_NAME = "chest_name";
    private static final String F_CHEST_MAX = "chest_max_damage";
    private static final String F_GLIDER_ENCHANTED = "glider_has_enchantments";
    private static final String F_CHEST_SWAPPABLE = "chest_original_swappable";

    /** 该堆是否带有合并状态（仅检查 custom_data 内是否存在本模组键，不解析内容）。 */
    public static boolean has(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) {
            return false;
        }
        return custom.copyTag().get(KEY) instanceof CompoundTag;
    }

    /** 读取标量字段（两端安全：不触碰快照 / 名称子标签）。字段缺失 / 类型不符时返回 empty。 */
    public static Optional<ElytraChestplateState> read(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || custom.isEmpty() || !(custom.copyTag().get(KEY) instanceof CompoundTag state)) {
            return Optional.empty();
        }
        Optional<String> gliderItem = state.getString(F_GLIDER_ITEM);
        Optional<Integer> gliderMax = state.getInt(F_GLIDER_MAX);
        if (gliderItem.isEmpty() || gliderItem.get().isEmpty() || gliderMax.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new ElytraChestplateState(
                gliderItem.get(),
                state.getIntOr(F_GLIDER_DAMAGE, 0), gliderMax.get(),
                state.get(F_GLIDER_NAME), state.get(F_CHEST_NAME),
                state.getIntOr(F_CHEST_MAX, -1), state.getBooleanOr(F_GLIDER_ENCHANTED, false),
                state.getBooleanOr(F_CHEST_SWAPPABLE, true)));
    }

    /** 合并落账时一次性写入（保留 custom_data 中其他键）。 */
    public static void write(ItemStack stack, ElytraChestplateState state, Tag gliderSnapshot) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            CompoundTag compound = new CompoundTag();
            compound.putString(F_GLIDER_ITEM, state.gliderItem());
            compound.putInt(F_GLIDER_DAMAGE, state.gliderDamage());
            compound.putInt(F_GLIDER_MAX, state.gliderMaxDamage());
            putIfPresent(compound, F_GLIDER_NAME, state.gliderName());
            putIfPresent(compound, F_CHEST_NAME, state.chestName());
            compound.putInt(F_CHEST_MAX, state.chestMaxDamage());
            compound.putBoolean(F_GLIDER_ENCHANTED, state.gliderHasEnchantments());
            compound.putBoolean(F_CHEST_SWAPPABLE, state.chestOriginalSwappable());
            compound.put(GLIDER_SNAPSHOT, gliderSnapshot);
            tag.put(KEY, compound);
        });
    }

    private static void putIfPresent(CompoundTag compound, String key, Tag value) {
        if (value != null) {
            compound.put(key, value);
        }
    }

    /** 胸甲升级后更新新物品原本的 swappable 值，拆解时恢复。 */
    public static void updateChestSwappable(ItemStack stack, boolean swappable) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (tag.get(KEY) instanceof CompoundTag compound) {
                compound.putBoolean(F_CHEST_SWAPPABLE, swappable);
            }
        });
    }

    /** 胸甲升级后更新胸甲自身的耐久上限，不改动鞘翅快照。 */
    public static void updateChestMaxDamage(ItemStack stack, int chestMaxDamage) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (tag.get(KEY) instanceof CompoundTag compound) {
                compound.putInt(F_CHEST_MAX, Math.max(0, chestMaxDamage));
            }
        });
    }

    /** 耐久记账更新（服务端每次实际损耗后调用）：只改鞘翅部分的损伤整数，其余原样保留。 */
    public static void updateGliderDamage(ItemStack stack, int gliderDamage) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (tag.get(KEY) instanceof CompoundTag compound) {
                compound.putInt(F_GLIDER_DAMAGE, gliderDamage);
            }
        });
    }

    /** 拆解：抹掉本模组记账（custom_data 里没有别的键时组件本身也会被移除）。 */
    public static void clear(ItemStack stack) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.remove(KEY));
    }

    /** 服务端：解析鞘翅快照（含附魔 / 纹饰 / 原名称，不含耐久组件）。 */
    public static Optional<ItemStack> readSnapshot(ItemStack stack, HolderLookup.Provider provider) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null || custom.isEmpty() || !(custom.copyTag().get(KEY) instanceof CompoundTag state)) {
            return Optional.empty();
        }
        Tag snapshot = state.get(GLIDER_SNAPSHOT);
        if (snapshot == null) {
            return Optional.empty();
        }
        return ItemStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), snapshot).result();
    }

    /** 服务端：把快照堆编码为可写入 custom_data 的 NBT 标签。 */
    public static Optional<Tag> encodeSnapshot(ItemStack snapshot, HolderLookup.Provider provider) {
        return ItemStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), snapshot).result();
    }

    /**
     * 把名称组件编码为 NBT（{@code null} 原样返回 {@code null}）。
     * <p>物品名称只会是文本 / 翻译组件，不需要注册表上下文，因此两端都能编解码。</p>
     */
    public static Tag encodeName(Component name) {
        if (name == null) {
            return null;
        }
        return ComponentSerialization.CODEC.encodeStart(NbtOps.INSTANCE, name).result().orElse(null);
    }

    /** 把记账里的名称标签解码回组件（缺失 / 解码失败时返回 empty）。 */
    public static Optional<Component> decodeName(Tag name) {
        if (name == null) {
            return Optional.empty();
        }
        return ComponentSerialization.CODEC.parse(NbtOps.INSTANCE, name).result();
    }
}
