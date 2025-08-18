package me.enderkill98.proxchat.mixin.client.mods.emotecraft;

import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.network.ClientEmotePlay;
import me.enderkill98.proxchat.Config;
import me.enderkill98.proxchat.Packets;
import me.enderkill98.proxlib.client.ProxLib;
import net.minecraft.client.MinecraftClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(value = ClientEmotePlay.class, remap = false)
public class EmotecraftClientEmotePlayMixin {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("ProxChat/EmotecraftClientEmotePlayMixin");

    @Unique private static void sendPacket(String logMessageStart, byte[] data) {
        if(!Config.HANDLER.instance().sendEmotes.should()) return;

        int packets = ProxLib.sendPacket(MinecraftClient.getInstance(), Packets.PACKET_ID_EMOTECRAFT, data);
        LOGGER.info("{} => {} packets", logMessageStart, packets);
    }

    @Inject(at = @At("RETURN"), method = "clientStartLocalEmote(Ldev/kosmx/playerAnim/core/data/KeyframeAnimation;I)Z")
    private static void clientStartLocalEmote(KeyframeAnimation emote, int tick, CallbackInfoReturnable<Boolean> info) {
        EmoteHolder holder = findEmoteHolder(emote);
        if(holder == null) {
            LOGGER.warn("Failed to identify Emotecraft emote from Keyframes!");
            return;
        }
        sendPacket("Sending start of Emotecraft-Emote: " + holder.name.getString() + " (" + holder.getUuid() + ", tick " + tick + ")",
                Packets.createEmotecraftPacket(new Packets.EmotecraftData(Packets.EmotecraftAction.StartEmote, holder.getUuid(), tick))
        );
    }

    @Inject(at = @At("RETURN"), method = "clientRepeatLocalEmote")
    private static void clientRepeatLocalEmote(KeyframeAnimation emote, int tick, UUID target, CallbackInfo ci) {
        EmoteHolder holder = findEmoteHolder(emote);
        if(holder == null) {
            LOGGER.warn("Failed to identify Emotecraft emote from Keyframes!");
            return;
        }
        sendPacket("Sending repeat of Emotecraft-Emote: " + holder.name.getString() + " (" + holder.getUuid() + ", tick " + tick + ")",
                Packets.createEmotecraftPacket(new Packets.EmotecraftData(Packets.EmotecraftAction.RepeatEmote, holder.getUuid(), tick))
        );
    }

    @Inject(at = @At("RETURN"), method = "clientStopLocalEmote(Ldev/kosmx/playerAnim/core/data/KeyframeAnimation;)Z")
    private static void clientStopLocalEmote(KeyframeAnimation emote, CallbackInfoReturnable<Boolean> info) {
        EmoteHolder holder = findEmoteHolder(emote);
        if(holder == null) {
            LOGGER.warn("Failed to identify Emotecraft emote from Keyframes!");
            return;
        }
        sendPacket("Sending stop of Emotecraft-Emote: " + holder.name.getString() + " (" + holder.getUuid() + ")",
                Packets.createEmotecraftPacket(new Packets.EmotecraftData(Packets.EmotecraftAction.StopEmote, holder.getUuid(), 0/*Doesn't matter*/))
        );
    }

    @Unique private static @Nullable EmoteHolder findEmoteHolder(KeyframeAnimation keyframeAnimation) {
        for(EmoteHolder holder : EmoteHolder.list) {
            if(holder.emote.equals(keyframeAnimation))
                return holder;
        }
        return null;
    }

}
