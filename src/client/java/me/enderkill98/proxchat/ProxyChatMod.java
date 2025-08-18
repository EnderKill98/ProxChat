package me.enderkill98.proxchat;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import me.enderkill98.proxlib.ProxPacketIdentifier;
import me.enderkill98.proxlib.client.ProxLib;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;

public class ProxyChatMod implements ClientModInitializer, ClientTickEvents.StartTick {
	public static final Logger LOGGER = LoggerFactory.getLogger("ProxChat");
	public static final String PREFIX = "§8[§aProxChat§8] §f";
	public static boolean hasBrotli = false;
	public static boolean hasOnlineEmotes = false;

	public static HashSet<UUID/*Sender*/> ignoreLegacyPatsFrom = new HashSet<>();

	@Override
	public void onInitializeClient() {
		try {
			Brotli4jLoader.ensureAvailability();
			if (!Brotli4jLoader.isAvailable())
				throw new RuntimeException("Brotli is not available (maybe not for your OS/Arch).");
			hasBrotli = true;
		}catch (Throwable ex) {
			LOGGER.warn("Failed to Load Brotli library. Some packets, that have compression, will not work!");
			hasBrotli = false;
		}

		Config.HANDLER.load();
		Config.HANDLER.instance().nullsToDefault();

		ClientTickEvents.START_CLIENT_TICK.register(this);

		LOGGER.info("ProxChat PatPat-Integration is no longer sending Pat's. Just display received ones for ProxChat (from older clients). Please check PatPat's mod settings and make sure it's enabled and tell others to update PatPat.");

		if(FabricLoader.getInstance().isModLoaded("online_emotes")) {
			hasOnlineEmotes = true;
			LOGGER.info("ProxChat detected online_emotes to be installed. Will not send EmoteCraft packets, as that mod does it already! Others will need online_emotes installed as well to see your Emotes.");
		}

		ProxLib.addHandlerFor(Packets.PACKET_ID_CHAT, this::handleChatPacket);
		ProxLib.addHandlerFor(Packets.PACKET_ID_TEXTDISPLAY, this::handleTextDisplayPacket);

		if(FabricLoader.getInstance().isModLoaded("patpat")) {
			ProxLib.addHandlerFor(Packets.PACKET_ID_PATPAT_PATENTITY, this::handlePatPatPatEntityPacket);
			ProxLib.addHandlerFor(ProxPacketIdentifier.of(2/*PatPat*/, 0), this::handleNativePatPatPatEntityPacket);
		}

		if(FabricLoader.getInstance().isModLoaded("emotecraft"))
			ProxLib.addHandlerFor(Packets.PACKET_ID_EMOTECRAFT, this::handleEmotecraftEmotePacket);
	}

	public static void displayChatMessage(MinecraftClient client, PlayerEntity sender, String message) {
		if(client.player != null)
			client.player.sendMessage(Text.of(ProxyChatMod.PREFIX + "§2" + sender.getGameProfile().getName() + ": §a" + message), false);
	}

	@Override
	public void onStartTick(MinecraftClient client) {
		TextDisplay.State.tickAll(client.world);
	}

	public void handleChatPacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		String message = Packets.readChatPacket(data);
		displayChatMessage(MinecraftClient.getInstance(), sender, message);
	}

	public void handlePatPatPatEntityPacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		if (!Config.HANDLER.instance().displayLegacyPats.should(sender.getUuid())) return;

		final MinecraftClient client = MinecraftClient.getInstance();
		int pattedEntityId = Packets.readPatPatPatEntityPacket(data);
		Entity patted = client.world.getEntityById(pattedEntityId);
		if(!(patted instanceof LivingEntity pattedLivingEntity)) return;
		PatPatInjector.handlePatted(client, pattedLivingEntity, sender);
	}

	public void handleNativePatPatPatEntityPacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		if(ignoreLegacyPatsFrom.add(sender.getUuid()))
			LOGGER.info("Ignoring LegacyPats from {} because they sent a native Pat-Packet.", sender.getGameProfile().getName());
	}

	public void handleEmotecraftEmotePacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		if (!Config.HANDLER.instance().displayEmotes.should()) return;

		Packets.EmotecraftData ecData = Packets.readEmotecraftPacket(data);
		switch(ecData.action()) {
			case StartEmote, RepeatEmote -> EmotecraftInjector.startEmote(sender, ecData.emoteUuid(), ecData.tick());
			case StopEmote -> EmotecraftInjector.stopEmote(sender, ecData.emoteUuid());
		}
	}

	public void handleTextDisplayPacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		TextDisplay.TextDisplayPacket packet = Packets.readTextDisplayPacket(data);
		TextDisplay.TextDisplayPacket.handle(MinecraftClient.getInstance(), Vec3d.of(sender.getBlockPos()), sender, packet.commands());
	}

}