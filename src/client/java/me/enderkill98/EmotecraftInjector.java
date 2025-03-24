package me.enderkill98;

import io.github.kosmx.emotes.PlatformTools;
import io.github.kosmx.emotes.api.events.client.ClientEmoteEvents;
import io.github.kosmx.emotes.api.events.server.ServerEmoteAPI;
import io.github.kosmx.emotes.arch.network.client.ClientNetwork;
import io.github.kosmx.emotes.common.network.EmotePacket;
import io.github.kosmx.emotes.main.EmoteHolder;
import io.github.kosmx.emotes.main.mixinFunctions.IPlayerEntity;
import io.github.kosmx.emotes.main.network.ClientEmotePlay;
import io.github.kosmx.emotes.server.network.EmotePlayTracker;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;

public class EmotecraftInjector {

    private final Identifier EMOTE = Identifier.of("emotecraft", "emote");
    private final Identifier STREAM = Identifier.of("emotecraft", "stream");

    public void injectEmotecraftPacket(Identifier id, ByteBuffer bytes) throws IOException {
        if(id.equals(EMOTE))
            ClientNetwork.INSTANCE.receiveConfigMessage(bytes, ClientPlayNetworking.getSender()::sendPacket);
        else if(id.equals(STREAM))
            ClientNetwork.INSTANCE.receiveStreamMessage(bytes, ClientPlayNetworking.getSender()::sendPacket);
        else
            throw new RuntimeException("ProxChat: Can't inject Emotecraft packet, because identifier is unexpected/unsupported by ProxyChat: " + id);
    }

    public static void startEmote(PlayerEntity emotingPlayer, UUID emoteUuid, int tick) {
        if(emoteUuid == null) {
            ProxFormat.LOGGER.warn("Starting/Repeating Emote without emoteUuid is currently not supported.");
            return;
        }

        EmoteHolder holder = EmoteHolder.getEmoteFromUuid(emoteUuid);
        if(holder == null) {
            ProxFormat.LOGGER.info("Failed to find EmoteHolder by UUID: " + emoteUuid + " (you may not have this emote installed)");
            return;
        }
        ClientEmoteEvents.EMOTE_PLAY.invoker().onEmotePlay(holder.emote, tick, emotingPlayer.getUuid());
        ((IPlayerEntity) emotingPlayer).emotecraft$playEmote(holder.emote, 0, false);
        ProxFormat.LOGGER.info("Player " + emotingPlayer.getGameProfile().getName() + " is starting/repeating emote " + holder.name.getString() + " (" + holder.getUuid() + ")...");
    }

    public static void stopEmote(PlayerEntity emotingPlayer, UUID emoteUuid) {
        ClientEmoteEvents.EMOTE_STOP.invoker().onEmoteStop(emoteUuid, emotingPlayer.getUuid());
        if(emoteUuid != null)
            ((IPlayerEntity) emotingPlayer).stopEmote(emoteUuid);
        else
            ((IPlayerEntity) emotingPlayer).stopEmote();
        ProxFormat.LOGGER.info("Player " + emotingPlayer.getGameProfile().getName() + " stopped emote " + (emotingPlayer == null ? "without an UUID" : "with UUID " + emoteUuid) + ".");
    }

}
