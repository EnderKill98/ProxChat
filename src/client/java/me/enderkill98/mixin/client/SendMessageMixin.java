package me.enderkill98.mixin.client;

import me.enderkill98.ProxFormat;
import me.enderkill98.ProxyChatMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public class SendMessageMixin {

    @Inject(at = @At("HEAD"), method = "sendMessage", cancellable = true)
    private void sendMessage(String chatText, boolean addToHistory, CallbackInfo info) {
        if (chatText.startsWith("% ")) {
            MinecraftClient client = MinecraftClient.getInstance();
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
            if(addToHistory) client.inGameHud.getChatHud().addToMessageHistory(chatText);
            info.cancel();
        }
    }

}
