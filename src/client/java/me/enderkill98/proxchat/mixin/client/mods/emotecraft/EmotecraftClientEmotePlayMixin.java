package me.enderkill98.proxchat.mixin.client.mods.emotecraft;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.network.ClientEmotePlay;
import me.enderkill98.proxchat.ProxFormat;
import me.enderkill98.proxchat.ProxyChatMod;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = ClientEmotePlay.class, remap = false)
public class EmotecraftClientEmotePlayMixin {

    @Unique private static void sendPacket(byte[] data) {
        int packets = ProxyChatMod.sendPacket(MinecraftClient.getInstance(), ProxFormat.ProxPackets.PACKET_ID_EMOTECRAFT, data);
        ProxFormat.LOGGER.info("Sent Emotecraft message with " + packets + " packets!");
    }

    @Inject(at = @At("RETURN"), method = "clientStartLocalEmote(Ldev/kosmx/playerAnim/core/data/KeyframeAnimation;I)Z")
    private static void clientStartLocalEmote(KeyframeAnimation emote, int tick, CallbackInfoReturnable<Boolean> info) {
        EmoteHolder holder = findEmoteHolder(emote);
        if(holder == null) {
            ProxFormat.LOGGER.warn("Failed to identify Emotecraft emote from Keyframes!");
            return;
        }
        ProxFormat.LOGGER.info("Sending start of Emotecraft-Emote: " + holder.name.getString() + " (" + holder.getUuid() + ", tick " + tick + ")");
        sendPacket(ProxFormat.ProxPackets.createEmotecraftPacket(new ProxFormat.ProxPackets.EmotecraftData(ProxFormat.ProxPackets.EmotecraftAction.StartEmote, holder.getUuid(), tick)));
    }

    @Inject(at = @At("RETURN"), method = "clientRepeatLocalEmote")
    private static void clientRepeatLocalEmote(KeyframeAnimation emote, int tick, UUID target, CallbackInfo ci) {
        EmoteHolder holder = findEmoteHolder(emote);
        if(holder == null) {
            ProxFormat.LOGGER.warn("Failed to identify Emotecraft emote from Keyframes!");
            return;
        }
        ProxFormat.LOGGER.info("Sending repeat of Emotecraft-Emote: " + holder.name.getString() + " (" + holder.getUuid() + ", tick " + tick + ")");
        sendPacket(ProxFormat.ProxPackets.createEmotecraftPacket(new ProxFormat.ProxPackets.EmotecraftData(ProxFormat.ProxPackets.EmotecraftAction.RepeatEmote, holder.getUuid(), tick)));
    }

    @Inject(at = @At("RETURN"), method = "clientStopLocalEmote(Ldev/kosmx/playerAnim/core/data/KeyframeAnimation;)Z")
    private static void clientStopLocalEmote(KeyframeAnimation emote, CallbackInfoReturnable<Boolean> info) {
        EmoteHolder holder = findEmoteHolder(emote);
        if(holder == null) {
            ProxFormat.LOGGER.warn("Failed to identify Emotecraft emote from Keyframes!");
            return;
        }
        ProxFormat.LOGGER.info("Sending stop of Emotecraft-Emote: " + holder.name.getString() + " (" + holder.getUuid() + ")");
        sendPacket(ProxFormat.ProxPackets.createEmotecraftPacket(new ProxFormat.ProxPackets.EmotecraftData(ProxFormat.ProxPackets.EmotecraftAction.StopEmote, holder.getUuid(), 0/*Doesn't matter*/)));
    }

    @Unique private static @Nullable EmoteHolder findEmoteHolder(KeyframeAnimation keyframeAnimation) {
        for(EmoteHolder holder : EmoteHolder.list) {
            if(holder.emote.equals(keyframeAnimation))
                return holder;
        }
        return null;
    }

}
