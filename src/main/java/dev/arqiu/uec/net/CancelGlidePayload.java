package dev.arqiu.uec.net;

import dev.arqiu.uec.UniversalElytraChestplates;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * 客户端 → 服务端：玩家请求取消滑翔（点按疾跑键）。
 * 不携带任何状态，服务端只按玩家自身状态校验，天然防伪造。
 */
public record CancelGlidePayload() implements CustomPacketPayload {
    public static final CancelGlidePayload INSTANCE = new CancelGlidePayload();
    public static final Type<CancelGlidePayload> TYPE =
            new Type<>(UniversalElytraChestplates.id("cancel_glide"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CancelGlidePayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
