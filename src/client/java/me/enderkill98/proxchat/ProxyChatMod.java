package me.enderkill98.proxchat;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import me.enderkill98.proxchat.mixin.client.mods.patpat.PatPatClientPacketManagerInvoker;
import me.enderkill98.proxlib.ProxPacketIdentifier;
import me.enderkill98.proxlib.client.ProxLib;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyChatMod implements ClientModInitializer, ClientTickEvents.StartTick {
	public static final Logger LOGGER = LoggerFactory.getLogger("ProxChat");
	public static final String PREFIX = "§8[§aProxChat§8] §f";
	public static boolean hasBrotli = false;
	public static boolean integrateWithPatPat = true;

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

		ClientTickEvents.START_CLIENT_TICK.register(this);

		if(!ProxLib.getHandlersFor(Packets.PACKET_ID_PATPAT_PATENTITY).isEmpty()) {
			LOGGER.info("PatPat likely is handling Pat on its own. Not doing PatPat integration in ProxChat!");
		}

		ProxLib.addHandlerFor(Packets.PACKET_ID_CHAT, this::handleChatPacket);
		ProxLib.addHandlerFor(Packets.PACKET_ID_TEXTDISPLAY, this::handleTextDisplayPacket);

		if(integrateWithPatPat && FabricLoader.getInstance().isModLoaded("patpat"))
			ProxLib.addHandlerFor(Packets.PACKET_ID_PATPAT_PATENTITY, this::handlePatPatPatEntityPacket);

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
		int pattedEntityId = Packets.readPatPatPatEntityPacket(data);
		Entity patted = MinecraftClient.getInstance().world.getEntityById(pattedEntityId);
		if(patted == null) return;
		PatPatClientPacketManagerInvoker.handlePatted(sender.getUuid(), patted.getUuid(), false);
	}

	public void handleEmotecraftEmotePacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		Packets.EmotecraftData ecData = Packets.readEmotecraftPacket(data);
		switch(ecData.action()) {
			case StartEmote, RepeatEmote -> EmotecraftInjector.startEmote(sender, ecData.emoteUuid(), ecData.tick());
			case StopEmote -> EmotecraftInjector.stopEmote(sender, ecData.emoteUuid());
		}
	}

	public void handleTextDisplayPacket(PlayerEntity sender, ProxPacketIdentifier identifier, byte[] data) {
		TextDisplay.TextDisplayPacket packet = Packets.readTextDisplayPacket(data);
		TextDisplay.TextDisplayPacket.handle(MinecraftClient.getInstance(), null, sender, packet.commands());
	}

}