package me.enderkill98.mixin.client;

import com.aayushatharva.brotli4j.encoder.Encoder;
import me.enderkill98.ProxFormat;
import me.enderkill98.ProxyChatMod;
import me.enderkill98.TextDisplay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class SendMessageMixin {

    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    private void sendMessage(String chatText, boolean addToHistory, CallbackInfo info) {
        if(chatText.startsWith("%highlightOn")) {
            if(addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            ProxyChatMod.highlightSelectedBlock = true;
            info.cancel();
        }else if(chatText.startsWith("%highlightOff")) {
            if (addToHistory) MinecraftClient.getInstance().inGameHud.getChatHud().addToMessageHistory(chatText);
            ProxyChatMod.highlightSelectedBlock = false;
            info.cancel();
        }else if(chatText.startsWith("%%")) {
            String text = chatText.startsWith("%% ") ? chatText.substring("%% ".length()) : "Hello!";

            MinecraftClient client = MinecraftClient.getInstance();
            if(addToHistory) client.inGameHud.getChatHud().addToMessageHistory(chatText);
            Vec3d absPos = client.player.getEyePos().add(0, 1, 0);
            TextDisplay.Command[] commands = new TextDisplay.Command[] {
                    new TextDisplay.Command.SetRelativePos(TextDisplay.RelativePos.fromAbsolutePos(client.player, absPos)),
                    new TextDisplay.Command.SetRemovalTimeout(7500),
                    new TextDisplay.Command.SetDisplayFlags(false, false, true, DisplayEntity.TextDisplayEntity.TextAlignment.LEFT),
                    new TextDisplay.Command.SetText(text),
            };

            byte[] dataUncompresed = ProxFormat.ProxPackets.createTextDisplayPacket(new TextDisplay.TextDisplayPacket(TextDisplay.Compression.None, commands), false, null);
            byte[] dataBrotli = ProxFormat.ProxPackets.createTextDisplayPacket(new TextDisplay.TextDisplayPacket(TextDisplay.Compression.Brotli, commands), false, Encoder.Mode.TEXT);
            byte[] data;
            if(dataBrotli.length < dataUncompresed.length) {
                ProxyChatMod.LOGGER.info("Sending compressed (" + dataBrotli.length + "/" + dataUncompresed.length + "): " + text);
                data = dataBrotli;
            }else {
                ProxyChatMod.LOGGER.info("Sending uncompressed (" + dataBrotli.length + "/" + dataUncompresed.length + "): " + text);
                data = dataUncompresed;
            }

            short id = ProxFormat.ProxPackets.PACKET_ID_TEXTDISPLAY;
            int packets = 0;
            for(int pdu : ProxFormat.ProxPackets.fullyEncodeProxPacketToProxDataUnits(id, data)) {
                packets++;
                //logger.info("PDU: " + pdu);
                client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxFormat.ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
            }

            // Show for self:
            TextDisplay.TextDisplayPacket.handle(client, Vec3d.of(client.player.getBlockPos()), client.player, commands);
            info.cancel();
        }
        if (chatText.startsWith("% ")) {
            MinecraftClient client = MinecraftClient.getInstance();
            if(addToHistory) client.inGameHud.getChatHud().addToMessageHistory(chatText);
            if(client.player == null) return; // null-paranoia
            chatText = chatText.substring("% ".length());

            short id = ProxFormat.ProxPackets.PACKET_ID_CHAT;
            byte[] data = ProxFormat.ProxPackets.createChatPacket(chatText);

            int packets = 0;
            for(int pdu : ProxFormat.ProxPackets.fullyEncodeProxPacketToProxDataUnits(id, data)) {
                packets++;
                //logger.info("PDU: " + pdu);
                client.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, ProxFormat.ProxDataUnits.proxDataUnitToBlockPos(client.player, pdu), Direction.DOWN));
            }
            ProxFormat.LOGGER.info("Sent chat message with " + packets + " packets!");
            ProxyChatMod.displayChatMessage(client, client.player, chatText);
            info.cancel();
        }
    }

}
