package me.enderkill98;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
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
			}
		});
	}

}