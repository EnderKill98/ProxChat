package me.enderkill98.proxchat;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyChatMod implements ClientModInitializer, ClientTickEvents.StartTick {
	public static final Logger LOGGER = LoggerFactory.getLogger("ProxChat");
	public static final String PREFIX = "§8[§aProxChat§8] §f";
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

	@Override
	public void onStartTick(MinecraftClient client) {
		TextDisplay.State.tickAll(client.world);
	}

}