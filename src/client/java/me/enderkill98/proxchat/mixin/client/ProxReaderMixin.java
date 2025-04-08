package me.enderkill98.proxchat.mixin.client;

import me.enderkill98.proxchat.ProxFormat;
import me.enderkill98.proxchat.ProxyChatMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashMap;

@Mixin(ClientPlayNetworkHandler.class)
public class ProxReaderMixin {

	@Unique public HashMap<PlayerEntity, ProxFormat.ProxPlayerReader> readers = new HashMap<>();
	@Unique private long lastCleanedUpAt = -1L;

	@Unique private void cleanup() {
		ArrayList<PlayerEntity> cleanupThese = new ArrayList<>();
		for(PlayerEntity player : readers.keySet()) {
			if(player.isRemoved())
				cleanupThese.add(player);
		}
		cleanupThese.forEach(readers::remove);
	}

	@Unique private void reset() {
		readers.clear();
	}

	@Inject(at = @At("TAIL"), method = "onPlayerRespawn")
	public void onPlayerRespawn(CallbackInfo info) {
		reset();
	}

	@Inject(at = @At("TAIL"), method = "onGameJoin")
	public void onGameJoin(CallbackInfo info) {
		reset();
	}

	@Inject(at = @At("TAIL"), method = "clearWorld")
	public void clearWorld(CallbackInfo info) {
		reset();
	}

	@Inject(at = @At("TAIL"), method = "onWorldTimeUpdate")
	public void onWorldTimeUpdate(CallbackInfo info) {
		if(lastCleanedUpAt == -1L || System.currentTimeMillis() - lastCleanedUpAt >= 750) {
			cleanup();
			lastCleanedUpAt = System.currentTimeMillis();
		}
	}

	@Inject(at = @At("TAIL"), method = "onBlockBreakingProgress")
	public void onBlockBreakingProgress(BlockBreakingProgressS2CPacket packet, CallbackInfo info) {
		//LOGGER.info("E: " + packet.getEntityId() + ", Pos: " + packet.getPos() + ", P: " + packet.getProgress());
		if(packet.getProgress() != 255) return; // Sent for Abort Dig Action

		MinecraftClient client = MinecraftClient.getInstance();
		if(client.player == null || client.world == null) return;
		Entity senderEntity = client.world.getEntityById(packet.getEntityId());
		if(!(senderEntity instanceof PlayerEntity sender)) return;

		ProxFormat.ProxPlayerReader reader;
		if(!readers.containsKey(sender)) {
			reader = new ProxFormat.ProxPlayerReader(sender);
			ProxyChatMod.addHandlers(reader);
			readers.put(sender, reader);
		}else {
			reader = readers.get(sender);
		}

		reader.handle(packet);
	}

}
