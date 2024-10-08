package me.enderkill98.mixin.client.mods.patpat;

import me.enderkill98.ProxFormat;
import net.lopymine.patpat.packet.PatEntityC2SPacket;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
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

            short id = ProxFormat.ProxPackets.PACKET_ID_PATPAT_PATENTITY;
            byte[] data = ProxFormat.ProxPackets.createPatPatPatEntityPacket(pattedEntity.getId());

            int packets = 0;
            for(int pdu : ProxFormat.ProxPackets.fullyEncodeProxPacketToProxDataUnits(id, data)) {
                packets++;
                //logger.info("PDU: " + pdu);
                client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxFormat.ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
            }
            ProxFormat.LOGGER.info("Sent PatPat-PatEntity message with " + packets + " packets!");

        }
    }

}
