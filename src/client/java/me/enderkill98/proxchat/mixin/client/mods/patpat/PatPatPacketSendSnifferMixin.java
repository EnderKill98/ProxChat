package me.enderkill98.proxchat.mixin.client.mods.patpat;

import me.enderkill98.proxchat.Packets;
import me.enderkill98.proxchat.ProxyChatMod;
import me.enderkill98.proxlib.client.ProxLib;
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

    @Inject(at = @At("HEAD"), method = "sendPacket")
    private void sendPacket(Packet<?> packet, CallbackInfo ci) {
        if(!ProxyChatMod.integrateWithPatPat) return;
        if(!(packet instanceof CustomPayloadC2SPacket cpPacket) || !(cpPacket.payload() instanceof PatEntityC2SPacket patPayload)) return;

        Entity pattedEntity = null;
        for(Entity maybePatted : client.world.getEntities()) {
            if(maybePatted.getUuid().equals(patPayload.getPattedEntityUuid())) {
                pattedEntity = maybePatted;
                break;
            }
        }
        if(pattedEntity == null) return; // Not found

        int packets = ProxLib.sendPacket(client, Packets.PACKET_ID_PATPAT_PATENTITY, Packets.createPatPatPatEntityPacket(pattedEntity.getId()));
        ProxyChatMod.LOGGER.info("Sent PatPat-PatEntity message with " + packets + " packets!");
    }

}
