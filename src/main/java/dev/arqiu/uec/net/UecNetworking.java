package dev.arqiu.uec.net;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * 网络注册与处理。服务端收包已在主线程执行；滑翔状态只由服务端最终控制。
 */
public final class UecNetworking {
    private UecNetworking() {
    }

    /** 在 onInitialize 里调用（两端都会执行，注册 serverbound 包类型）。 */
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(CancelGlidePayload.TYPE, CancelGlidePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CancelGlidePayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            // 服务端验证：必须处于滑翔状态才允许取消（地面疾跑等场景无副作用）。
            if (player.isFallFlying()) {
                player.stopFallFlying();
            }
        });
    }
}
