package me.enderkill98.mixin.client.mods.patpat;

import net.lopymine.patpat.manager.client.PatPatClientPacketManager;
import org.apache.commons.lang3.NotImplementedException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.UUID;

@Mixin(value = PatPatClientPacketManager.class, remap = false)
public interface PatPatClientPacketManagerInvoker {

    @Invoker("handlePatting")
    static void handlePatted(UUID whoPattedUuid, UUID pattedEntityUuid, boolean replayModPacket) {
        throw new NotImplementedException();
    }

}
