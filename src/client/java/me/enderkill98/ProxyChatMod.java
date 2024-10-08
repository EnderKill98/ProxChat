package me.enderkill98;

import me.enderkill98.mixin.client.mods.patpat.PatPatClientPacketManagerInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyChatMod implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("ProxChat");
	public static final String PREFIX = "§8[§aProxChat§8] §f";

	@Override
	public void onInitializeClient() {}

	public static void displayChatMessage(MinecraftClient client, PlayerEntity sender, String message) {
		if(client.player != null)
			client.player.sendMessage(Text.of(ProxyChatMod.PREFIX + "§2" + sender.getGameProfile().getName() + ": §a" + message));
	}

	public static void addHandlers(ProxFormat.ProxPlayerReader reader) {
		reader.addHandler((id, data) -> {
			MinecraftClient client = MinecraftClient.getInstance();
			if(client.player == null) return;
			if(id == ProxFormat.ProxPackets.PACKET_ID_CHAT) {
				String message = ProxFormat.ProxPackets.readChatPacket(data);
				displayChatMessage(client, reader.getPlayer(), message);
			}else if(id == ProxFormat.ProxPackets.PACKET_ID_PATPAT_PATENTITY) {
				if(FabricLoader.getInstance().isModLoaded("patpat")) {
					Entity patter = reader.getPlayer();
					int pattedId = ProxFormat.ProxPackets.readPatPatPatEntityPacket(data);
					Entity patted = client.world.getEntityById(pattedId);
					if(patted != null)
						PatPatClientPacketManagerInvoker.handlePatted(patter.getUuid(), patted.getUuid(), false);
				}
			}
		});
	}
}