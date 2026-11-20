package dev.arqiu.uec.client;

import dev.arqiu.uec.component.ElytraChestplateState;
import dev.arqiu.uec.equipment.ElytraChestplate;
import dev.arqiu.uec.net.CancelGlidePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 客户端入口（模组为可选安装）。只负责三件事，避免把渲染 / 输入类加载到专用服务端：
 * <ol>
 *   <li>疾跑键点按（单次按下）时，若正在空中滑翔则向服务端发送取消滑翔请求；
 *       不修改原版按键绑定，最终状态由服务端决定，地面疾跑不受影响。</li>
 *   <li>为合并胸甲追加一行耐久明细提示（鞘翅部分剩余）。</li>
 *   <li>为可合并 / 可拆解的物品追加一行操作提示。</li>
 * </ol>
 * 双模型鞘翅渲染由 {@code dev.arqiu.uec.mixin.client.WingsLayerElytraAssetMixin} 实现。
 */
public final class UecClient implements ClientModInitializer {

    private boolean sprintWasDown;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        ItemTooltipCallback.EVENT.register(UecClient::appendElytraTooltip);
    }

    private void onClientTick(Minecraft mc) {
        boolean down = mc.options.keySprint.isDown();
        if (down && !sprintWasDown
                && mc.player != null
                && mc.player.isFallFlying()
                && !mc.player.onGround()) {
            ClientPlayNetworking.send(CancelGlidePayload.INSTANCE);
        }
        sprintWasDown = down;
    }

    private static void appendElytraTooltip(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                            net.minecraft.world.item.TooltipFlag flag, java.util.List<Component> lines) {
        ElytraChestplateState state = ElytraChestplateState.read(stack).orElse(null);
        if (state == null) {
            if (ElytraChestplate.isGlider(stack)) {
                // 可合并的鞘翅：给出操作提示（合并本身纯服务端裁决，无提示也生效）。
                lines.add(Component.translatable("item.universal_elytra_chestplates.tooltip.merge_hint")
                        .withStyle(ChatFormatting.GRAY));
            }
            return;
        }
        int[] split = ElytraChestplate.split(stack.getDamageValue(), state);
        int gliderLeft = Math.max(0, state.gliderMaxDamage() - split[1]);
        lines.add(Component.translatable("item.universal_elytra_chestplates.tooltip.glider",
                        gliderLeft, state.gliderMaxDamage())
                .withStyle(ChatFormatting.BLUE));
        lines.add(Component.translatable("item.universal_elytra_chestplates.tooltip.split_hint")
                .withStyle(ChatFormatting.GRAY));
    }
}
