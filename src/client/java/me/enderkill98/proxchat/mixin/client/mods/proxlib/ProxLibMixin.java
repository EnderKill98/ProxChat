package me.enderkill98.proxchat.mixin.client.mods.proxlib;

import me.enderkill98.proxchat.Config;
import me.enderkill98.proxchat.ProxChatMod;
import me.enderkill98.proxlib.ProxDataUnits;
import me.enderkill98.proxlib.ProxPacketIdentifier;
import me.enderkill98.proxlib.ProxPackets;
import me.enderkill98.proxlib.client.ProxLib;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(ProxLib.class)
public class ProxLibMixin {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("ProxChat/ProxLibMixin");

    @Inject(method = "sendPacket(Lnet/minecraft/client/MinecraftClient;Lme/enderkill98/proxlib/ProxPacketIdentifier;[BZ)I", at = @At("HEAD"), cancellable = true)
    private static void sendPacket(MinecraftClient client, ProxPacketIdentifier identifier, byte[] data, boolean dryRun, CallbackInfoReturnable<Integer> info) {
        if(!dryRun && Config.HANDLER.instance().queueProxLibPacketsForBetterPacketOrder) {
            final List<Integer> pdus = ProxPackets.fullyEncodeProxPacketToProxDataUnits(identifier, data);
            final ArrayList<PlayerActionC2SPacket> packets = new ArrayList<>(pdus.size());
            for(int pdu : pdus)
                packets.add(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
            synchronized (ProxChatMod.midTickRunnables) {
                ProxChatMod.midTickRunnables.add(() -> {
                    ClientPlayNetworkHandler handler = client.getNetworkHandler();
                    if(handler != null) packets.forEach(handler::sendPacket);
                });
            }
            LOGGER.info("Hooked ProxLib.sendPacket(VendorId={}, PacketId={}, {} bytes) and queued the actual sending.", identifier.vendorId(), identifier.packetId(), data.length);
            info.setReturnValue(packets.size());
        }
    }

}
