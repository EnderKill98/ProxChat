package me.enderkill98;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.Encoder;
import me.enderkill98.mixin.client.mods.patpat.PatPatClientPacketManagerInvoker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.logging.Log;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class ProxyChatMod implements ClientModInitializer, ClientTickEvents.StartTick {
	public static final Logger LOGGER = LoggerFactory.getLogger("ProxChat");
	public static final String PREFIX = "§8[§aProxChat§8] §f";

	public static boolean hasBrotli = false;

	public static boolean highlightSelectedBlock = false;
	private static @Nullable BlockPos lastHighlightedBlockPos = null;
	private static int ticksSinceLastSelectedBlockUpdated = 0;
	private static long pauseHighlightsUntil = -1L; // Rate limiting

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
		});
	}

	@Override
	public void onStartTick(MinecraftClient client) {
		TextDisplay.State.tickAll(client.world);

		if(client.player == null || client.world == null) {
			lastHighlightedBlockPos = null;
			return;
		}

		BlockPos origLastHighlightedBlockPos = lastHighlightedBlockPos;
		if(lastHighlightedBlockPos == null && !highlightSelectedBlock) return;
		BlockPos newHighlightedBlockPos = highlightSelectedBlock && client.crosshairTarget instanceof BlockHitResult blockHitResult ? blockHitResult.getBlockPos() : null;
		int packets = 0;
		if(lastHighlightedBlockPos != null && !lastHighlightedBlockPos.equals(newHighlightedBlockPos)) {
			// Remove last pos
			packets += setBlockHighlighted(client, lastHighlightedBlockPos, false);
			lastHighlightedBlockPos = null;
		}

		if((pauseHighlightsUntil == -1L || System.currentTimeMillis() >= pauseHighlightsUntil) && // Rate limit
				newHighlightedBlockPos != null && (!newHighlightedBlockPos.equals(origLastHighlightedBlockPos) || ticksSinceLastSelectedBlockUpdated >= 20)) {
			packets += setBlockHighlighted(client, newHighlightedBlockPos, true);
			ticksSinceLastSelectedBlockUpdated = 0;
			lastHighlightedBlockPos = newHighlightedBlockPos;
		}
		if(newHighlightedBlockPos != null)
			ticksSinceLastSelectedBlockUpdated++;

		if((pauseHighlightsUntil == -1L || System.currentTimeMillis() >= pauseHighlightsUntil) && packets > 0) {
			pauseHighlightsUntil = System.currentTimeMillis() + ((packets * 1000) / 250);
			LOGGER.info("Pause for " + ((packets * 1000) / 250) + "ms");
		}
	}

	private record FaceInfo(Vec3d pos, float yaw, float pitch, float minWidth) {}

	private int setBlockHighlighted(MinecraftClient client, BlockPos pos, boolean highlight) {
		ArrayList<TextDisplay.Command> commands = new ArrayList<>();

		Vec3d anchorPos = Vec3d.of(client.player.getBlockPos());
		if(client.player.getBlockPos().getManhattanDistance(pos) > 20) {
			anchorPos = Vec3d.of(pos);
			commands.add(new TextDisplay.Command.SetAnchorPos(anchorPos));
		}

		Vec3d basePos = Vec3d.of(pos);
		FaceInfo[] faces = new FaceInfo[] {
				new FaceInfo(basePos.add(0.0, 0.0, 0.5), 90, 0, 1),     // West Face
				new FaceInfo(basePos.add(0.5, 0.0, 1.0), 0, 0, 1),      // South Face
				new FaceInfo(basePos.add(1.0, 0.0, 0.5), -90, 0, 1),    // East Face
				new FaceInfo(basePos.add(0.5, 0.0, 0.0), 180, 0, 1),    // North Face
				new FaceInfo(basePos.add(0.5, 1.0, 1.0), 0, -90, 1),    // Top Face
				new FaceInfo(basePos.add(0.25, 0.0, 0.0), 0, 90, 0.5f), // Bottom Face Part 1
				new FaceInfo(basePos.add(0.75, 0.0, 0.0), 0, 90, 0.5f), // Bottom Face Part 2
		};

		for(FaceInfo face : faces) {
			commands.add(new TextDisplay.Command.SetRelativePos(TextDisplay.RelativePos.fromAbsolutePos(anchorPos, face.pos)));
			if(!highlight) {
				// Cause despawn
				commands.add(new TextDisplay.Command.SetText(""));
				continue;
			}

			commands.add(new TextDisplay.Command.SetMinWidth(face.minWidth));
			commands.add(new TextDisplay.Command.SetBillboardMode(DisplayEntity.BillboardMode.FIXED));
			commands.add(new TextDisplay.Command.SetDisplayFlags(false, true, false, DisplayEntity.TextDisplayEntity.TextAlignment.CENTER));
			if(face.pitch != 0) commands.add(new TextDisplay.Command.SetPitch(face.pitch));
			if(face.yaw != 0) commands.add(new TextDisplay.Command.SetYaw(face.yaw));
			commands.add(new TextDisplay.Command.SetText(" \n \n \n "));
			commands.add(new TextDisplay.Command.SetBackground(0xFF068900)); // Green full opacity
			commands.add(new TextDisplay.Command.SetRemovalTimeout(1500));
		}

		byte[] encoded = ProxFormat.ProxPackets.createTextDisplayPacket(new TextDisplay.TextDisplayPacket(TextDisplay.Compression.None, commands.toArray(TextDisplay.Command[]::new)), false, null);
		byte[] encodedComp = hasBrotli ? ProxFormat.ProxPackets.createTextDisplayPacket(new TextDisplay.TextDisplayPacket(TextDisplay.Compression.Brotli, commands.toArray(TextDisplay.Command[]::new)), false, Encoder.Mode.GENERIC) : null;
		if(encodedComp != null && encodedComp.length < encoded.length) {
			encoded = encodedComp;
		}

		short id = ProxFormat.ProxPackets.PACKET_ID_TEXTDISPLAY;
		int packets = 0;
		for(int pdu : ProxFormat.ProxPackets.fullyEncodeProxPacketToProxDataUnits(id, encoded)) {
			packets++;
			//logger.info("PDU: " + pdu);
			client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxFormat.ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
		}

		// Show for self:
		TextDisplay.TextDisplayPacket.handle(client, Vec3d.of(client.player.getBlockPos()), client.player, commands.toArray(TextDisplay.Command[]::new));

		ProxyChatMod.LOGGER.info("Block " + pos + ", highlight: " + highlight + ", PDUs: " + packets);
		return packets;
	}
}