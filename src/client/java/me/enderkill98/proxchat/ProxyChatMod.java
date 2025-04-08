package me.enderkill98.proxchat;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import me.enderkill98.proxchat.mixin.client.mods.patpat.PatPatClientPacketManagerInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ProxyChatMod implements ClientModInitializer, ClientTickEvents.StartTick {

	public interface ProxChatGlobalHandler {
		void onPacketReceived(PlayerEntity sender, short id, byte[] data);
	}

	public static final Logger LOGGER = LoggerFactory.getLogger("ProxChat");
	public static final String PREFIX = "§8[§aProxChat§8] §f";
	private static final ArrayList<ProxChatGlobalHandler> REGISTERED_GLOBAL_HANDLERS = new ArrayList<>();
	public static boolean hasBrotli = false;

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
	}

	public static void displayChatMessage(MinecraftClient client, PlayerEntity sender, String message) {
		if(client.player != null)
			client.player.sendMessage(Text.of(ProxyChatMod.PREFIX + "§2" + sender.getGameProfile().getName() + ": §a" + message), false);
	}

	public static void addHandlers(ProxFormat.ProxPlayerReader reader) {
		// ProxChat built-in packet handler
		reader.addHandler((id, data) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if(client.player == null) return;

			if(id == ProxFormat.ProxPackets.PACKET_ID_CHAT) {
				String message = ProxFormat.ProxPackets.readChatPacket(data);
				displayChatMessage(client, reader.getPlayer(), message);
			}else if(id == ProxFormat.ProxPackets.PACKET_ID_PATPAT_PATENTITY) {
				if(!FabricLoader.getInstance().isModLoaded("patpat")) return;
				Entity patter = reader.getPlayer();
				int pattedId = ProxFormat.ProxPackets.readPatPatPatEntityPacket(data);
				Entity patted = client.world.getEntityById(pattedId);
				if(patted != null)
					PatPatClientPacketManagerInvoker.handlePatted(patter.getUuid(), patted.getUuid(), false);
			}else if(id == ProxFormat.ProxPackets.PACKET_ID_EMOTECRAFT) {
				if(!FabricLoader.getInstance().isModLoaded("emotecraft")) return;
				ProxFormat.ProxPackets.EmotecraftData ecData = ProxFormat.ProxPackets.readEmotecraftPacket(data);
				switch(ecData.action()) {
					case StartEmote, RepeatEmote -> EmotecraftInjector.startEmote(reader.getPlayer(), ecData.emoteUuid(), ecData.tick());
					case StopEmote -> EmotecraftInjector.stopEmote(reader.getPlayer(), ecData.emoteUuid());
				}
			}else if(id == ProxFormat.ProxPackets.PACKET_ID_TEXTDISPLAY) {
				TextDisplay.TextDisplayPacket packet = ProxFormat.ProxPackets.readTextDisplayPacket(data);
				client.submit(() -> {
					TextDisplay.TextDisplayPacket.handle(client, null, reader.getPlayer(), packet.commands());
				});
			}

			// Call others
			for(ProxChatGlobalHandler handler : REGISTERED_GLOBAL_HANDLERS) {
				try {
					handler.onPacketReceived(reader.getPlayer(), id, data);
				}catch (Exception ex) {
					LOGGER.error("A registered global handler threw an exception when handling a packet (id: " + id + ", data: " + data.length + " bytes)!", ex);
				}
			}
		});
	}

	/**
	 * Add your own handler, which ProxChat will automatically call for all received packets
	 */
	public static boolean addGlobalHandler(ProxChatGlobalHandler handler) {
		if(REGISTERED_GLOBAL_HANDLERS.contains(handler)) return false;
		REGISTERED_GLOBAL_HANDLERS.add(handler);
		return true;
	}

	public static boolean removeGlobalHandler(ProxChatGlobalHandler handler) {
		return REGISTERED_GLOBAL_HANDLERS.remove(handler);
	}

	@Override
	public void onStartTick(MinecraftClient client) {
		TextDisplay.State.tickAll(client.world);
	}

	public static int sendPacket(MinecraftClient client, short id, byte[] data) {
		return sendPacket(client, id, data, false);
	}

	/**
	 * Helper-function to easier send out packets.
	 * @param id Packet ID
	 * @param data Packet Data
	 * @param dryRun Do not send any actual data
	 * @return The amount of Minecraft-Packets produced for sending this packet
	 */
	public static int sendPacket(MinecraftClient client, short id, byte[] data, boolean dryRun) {
		int packets = 0;
		for(int pdu : ProxFormat.ProxPackets.fullyEncodeProxPacketToProxDataUnits(id, data)) {
			packets++;
			if(!dryRun) {
				client.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxFormat.ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
			}
		}
		return packets;
	}

}