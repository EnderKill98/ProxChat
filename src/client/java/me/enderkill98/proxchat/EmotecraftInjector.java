package me.enderkill98.proxchat;

import io.github.kosmx.emotes.api.events.client.ClientEmoteEvents;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.UUID;

public class EmotecraftInjector {

    public static void startEmote(PlayerEntity emotingPlayer, UUID emoteUuid, int tick) {
        if(emoteUuid == null) {
            ProxyChatMod.LOGGER.warn("Starting/Repeating Emote without emoteUuid is currently not supported.");
            return;
        }

        EmoteHolder holder = EmoteHolder.getEmoteFromUuid(emoteUuid);
        if(holder == null) {
            ProxyChatMod.LOGGER.info("Failed to find EmoteHolder by UUID: " + emoteUuid + " (you may not have this emote installed)");
            return;
        }
        ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(holder.emote, tick, emotingPlayer.getUuid());
        ((IPlayerEntity) emotingPlayer).emotecraft$playEmote(holder.emote, 0, false);
        ProxyChatMod.LOGGER.info("Player " + emotingPlayer.getGameProfile().getName() + " is starting/repeating emote " + holder.name.getString() + " (" + holder.getUuid() + ")...");
    }

    public static void stopEmote(PlayerEntity emotingPlayer, UUID emoteUuid) {
        ClientEmoteEvents.EMOTE_STOP.invoker().onEmoteStop(emoteUuid, emotingPlayer.getUuid());
        if(emoteUuid != null)
            ((IPlayerEntity) emotingPlayer).stopEmote(emoteUuid);
        else
            ((IPlayerEntity) emotingPlayer).stopEmote();
        ProxyChatMod.LOGGER.info("Player " + emotingPlayer.getGameProfile().getName() + " stopped emote " + (emotingPlayer == null ? "without an UUID" : "with UUID " + emoteUuid) + ".");
    }

}
