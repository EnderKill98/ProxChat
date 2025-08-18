package me.enderkill98.proxchat.mixin.client;

import com.aayushatharva.brotli4j.encoder.Encoder;
import me.enderkill98.proxchat.Packets;
import me.enderkill98.proxchat.ProxChatMod;
import me.enderkill98.proxchat.TextDisplay;
import me.enderkill98.proxlib.client.ProxLib;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatScreen.class, priority = 999)
public class SendMessageMixin {

    @Unique private static final Logger LOGGER = LoggerFactory.getLogger("ProxChat/SendMessageMixin");

    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    private void sendMessage(String chatText, boolean addToHistory, CallbackInfo info) {
        if(chatText.startsWith("%%")) {
            String text = chatText.startsWith("%% ") ? chatText.substring("%% ".length()) : "Hello!";

            MinecraftClient client = MinecraftClient.getInstance();
            if(addToHistory) client.inGameHud.getChatHud().addToMessageHistory(chatText);
            TextDisplay.Command[] commands = new TextDisplay.Command[] {
                    new TextDisplay.Command.SetAnchorEntity(null, true, new TextDisplay.RelativePos(0, 1, 0)),
                    new TextDisplay.Command.SetRemovalTimeout(7500),
                    new TextDisplay.Command.SetDisplayFlags(false, false, true, DisplayEntity.TextDisplayEntity.TextAlignment.LEFT),
                    new TextDisplay.Command.SetText(text),
            };

            byte[] dataUncompresed = Packets.createTextDisplayPacket(new TextDisplay.TextDisplayPacket(TextDisplay.Compression.None, commands), null, null);
            byte[] dataBrotli = Packets.createTextDisplayPacket(new TextDisplay.TextDisplayPacket(TextDisplay.Compression.Brotli, commands), null, Encoder.Mode.TEXT);
            byte[] data;
            if(dataBrotli.length < dataUncompresed.length) {
                LOGGER.info("Sending compressed (" + dataBrotli.length + "/" + dataUncompresed.length + "): " + text);
                data = dataBrotli;
            }else {
                LOGGER.info("Sending uncompressed (" + dataBrotli.length + "/" + dataUncompresed.length + "): " + text);
                data = dataUncompresed;
            }

            ProxLib.sendPacket(client, Packets.PACKET_ID_TEXTDISPLAY, data);
            TextDisplay.TextDisplayPacket.handle(client, Vec3d.of(client.player.getBlockPos()), client.player, commands); // Show for self
            info.cancel();
        }else if (chatText.startsWith("% ")) {
            MinecraftClient client = MinecraftClient.getInstance();
            if(addToHistory) client.inGameHud.getChatHud().addToMessageHistory(chatText);
            if(client.player == null) return; // null-paranoia
            chatText = chatText.substring("% ".length());

            int packets = ProxLib.sendPacket(client, Packets.PACKET_ID_CHAT, Packets.createChatPacket(chatText));
            LOGGER.info("Sent chat message with " + packets + " packets!");
            ProxChatMod.displayChatMessage(client, client.player, chatText);
            info.cancel();
        }
    }

}
