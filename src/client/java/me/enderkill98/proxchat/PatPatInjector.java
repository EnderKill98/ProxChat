package me.enderkill98.proxchat;

import net.lopymine.patpat.client.config.resourcepack.PlayerConfig;
import net.lopymine.patpat.client.render.PatPatClientRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;

public class PatPatInjector {

    public static void handlePatted(MinecraftClient client, LivingEntity patted, PlayerEntity patter) {
        PatPatClientRenderer.PacketPat packet = new PatPatClientRenderer.PacketPat(patted, PlayerConfig.of(patter.getGameProfile().getName(), patter.getUuid()), client.player, false);
        PatPatClientRenderer.registerServerPacket(packet);
    }

}
