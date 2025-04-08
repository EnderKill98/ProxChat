package me.enderkill98.proxchat.mixin.client.mods.patpat;

import me.enderkill98.proxchat.ProxFormat;
import me.enderkill98.proxchat.ProxyChatMod;
import net.lopymine.patpat.packet.PatEntityC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class PatPatPacketSendSnifferMixin {

    @Shadow @Final protected MinecraftClient client;

    @Inject(at = @At("HEAD"), method = "sendPacket", cancellable = true)
    private void sendPacket(Packet<?> packet, CallbackInfo ci) {
        if(packet instanceof CustomPayloadC2SPacket cpPacket && cpPacket.payload() instanceof PatEntityC2SPacket patPayload) {
            Entity pattedEntity = null;
            for(Entity maybePatted : client.world.getEntities()) {
                if(maybePatted.getUuid().equals(patPayload.getPattedEntityUuid())) {
                    pattedEntity = maybePatted;
                    break;
                }
            }
            if(pattedEntity == null) return; // Not found

            int packets = ProxyChatMod.sendPacket(client, ProxFormat.ProxPackets.PACKET_ID_PATPAT_PATENTITY, ProxFormat.ProxPackets.createPatPatPatEntityPacket(pattedEntity.getId()));
            ProxFormat.LOGGER.info("Sent PatPat-PatEntity message with " + packets + " packets!");
        }
    }

}
