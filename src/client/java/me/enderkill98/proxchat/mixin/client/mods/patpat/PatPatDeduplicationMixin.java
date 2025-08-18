package me.enderkill98.proxchat.mixin.client.mods.patpat;

import me.enderkill98.proxchat.Config;
import me.enderkill98.proxchat.ProxChatMod;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.s2c.common.CustomPayloadS2CPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class PatPatDeduplicationMixin {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("ProxChat/PatPatDeduplicationMixin");

    @Inject(method = "onCustomPayload(Lnet/minecraft/network/packet/s2c/common/CustomPayloadS2CPacket;)V", at = @At(value = "RETURN"))
    private void onCustomPayload(CustomPayloadS2CPacket packet, CallbackInfo info) {
        if(packet.payload().getId().id().getNamespace().equals("patpat") && !ProxChatMod.patDisabledDueToServerPacket) {
            ProxChatMod.patDisabledDueToServerPacket = true;
            if(Config.HANDLER.instance().displayLegacyPats == Config.DisplayLegacyPats.Deduplicated)
                LOGGER.info("The server sent a Custom Payload on channel {}. Disabled display of legacy Pats, because there will likely be duplicated pats from the server.", packet.payload().getId().id());
        }
    }

}
