package me.enderkill98.proxchat.mixin.client.mods.patpat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.enderkill98.proxchat.Config;
import me.enderkill98.proxchat.Packets;
import me.enderkill98.proxlib.client.ProxLib;
import net.lopymine.patpat.client.render.PatPatClientRenderer;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PatPatClientRenderer.class, remap = false)
public class LegacyPatSenderMixin {

    @WrapOperation(method = "lambda$register$0", at = @At(value = "INVOKE", target = "Lnet/lopymine/patpat/client/packet/PatPatClientProxLibPacketManager;onPat(I)V"))
    private static void onPat(int pattedEntityId, Operation<Void> original) {
        original.call(pattedEntityId);
        if(Config.HANDLER.instance().sendLegacyPats) // Send this after PatPat sent the native packet, so deduplication can work immediately
            ProxLib.sendPacket(MinecraftClient.getInstance(), Packets.PACKET_ID_PATPAT_PATENTITY, Packets.createPatPatPatEntityPacket(pattedEntityId));
    }

}
