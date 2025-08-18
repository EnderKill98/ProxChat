package me.enderkill98.proxchat;

import io.github.kosmx.emotes.api.events.client.ClientEmoteEvents;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class EmotecraftInjector {

    private static final Logger LOGGER = LoggerFactory.getLogger("ProxChat/EmotecraftInjector");

    public static void startEmote(PlayerEntity emotingPlayer, UUID emoteUuid, int tick) {
        if(emoteUuid == null) {
            LOGGER.warn("Starting/Repeating Emote without emoteUuid is currently not supported.");
            return;
        }

        EmoteHolder holder = EmoteHolder.getEmoteFromUuid(emoteUuid);
        if(holder == null) {
            LOGGER.info("Failed to find EmoteHolder by UUID: " + emoteUuid + " (you may not have this emote installed)");
            return;
        }
        ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(holder.emote, tick, emotingPlayer.getUuid());
        ((IPlayerEntity) emotingPlayer).emotecraft$playEmote(holder.emote, 0, false);
        LOGGER.info("Player " + emotingPlayer.getGameProfile().getName() + " is starting/repeating emote " + holder.name.getString() + " (" + holder.getUuid() + ")...");
    }

    public static void stopEmote(PlayerEntity emotingPlayer, UUID emoteUuid) {
        ClientEmoteEvents.EMOTE_STOP.invoker().onEmoteStop(emoteUuid, emotingPlayer.getUuid());
        if(emoteUuid != null)
            ((IPlayerEntity) emotingPlayer).stopEmote(emoteUuid);
        else
            ((IPlayerEntity) emotingPlayer).stopEmote();
        LOGGER.info("Player " + emotingPlayer.getGameProfile().getName() + " stopped emote " + (emotingPlayer == null ? "without an UUID" : "with UUID " + emoteUuid) + ".");
    }

}
