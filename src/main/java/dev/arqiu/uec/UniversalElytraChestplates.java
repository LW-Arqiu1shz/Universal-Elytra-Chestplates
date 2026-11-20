package dev.arqiu.uec;

import dev.arqiu.uec.interaction.UecInteractions;
import dev.arqiu.uec.net.UecNetworking;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

/**
 * Universal Elytra Chestplates —— 通用鞘翅胸甲模组入口（服务端 + 客户端都会加载）。
 * <p>设计目标：纯服务端生效——服务器只装本模组 + Fabric API 即可，
 * 玩家客户端无需安装任何东西（原版 / NeoForge 客户端可直接进服游玩）。
 * 客户端安装本模组为增强体验：鞘翅翅膀渲染、合并双图标、耐久提示、按键取消滑翔。</p>
 * <p>为达成该目标，本模组不注册任何注册表条目：
 * 合并状态存于原版 {@code minecraft:custom_data} 组件，
 * 合并 / 拆解由服务端手持交互裁决（见 {@link UecInteractions}），
 * 全部网络流量均为原版协议内容。</p>
 * 功能按包模块化，方便向其他版本移植：
 * <ul>
 *   <li>{@code component}：合并状态的 custom_data 读写（纯标量 + 服务端快照）</li>
 *   <li>{@code equipment}：胸甲/鞘翅判定与合并、还原逻辑</li>
 *   <li>{@code interaction}：锻造台台面投料融合、潜行右键拆解、服务端代穿戴（服务端权威）</li>
 *   <li>{@code durability}：耐久损耗归属（飞行扣鞘翅、受击扣胸甲）</li>
 *   <li>{@code net}：取消滑翔网络负载（仅模组客户端使用，服务端收包）</li>
 *   <li>{@code mixin}：耐久重定向（通用）、鞘翅渲染资产重定向（客户端）</li>
 * </ul>
 */
public final class UniversalElytraChestplates implements ModInitializer {
    public static final String MOD_ID = "universal_elytra_chestplates";

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        UecInteractions.init();
        UecNetworking.init();
    }
}
