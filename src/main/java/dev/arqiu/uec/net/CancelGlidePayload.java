package dev.arqiu.uec.net;

import dev.arqiu.uec.UniversalElytraChestplates;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload.Type;

public record CancelGlidePayload() implements CustomPacketPayload {
   public static final CancelGlidePayload INSTANCE = new CancelGlidePayload();
   public static final Type<CancelGlidePayload> TYPE = new Type(UniversalElytraChestplates.id("cancel_glide"));
   public static final StreamCodec<RegistryFriendlyByteBuf, CancelGlidePayload> CODEC = StreamCodec.unit(INSTANCE);

   public Type<? extends CustomPacketPayload> type() {
      return TYPE;
   }
}
