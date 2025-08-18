package me.enderkill98.proxchat.mixin.client;

import me.enderkill98.proxchat.TextDisplay;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class TextDisplayGameRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    public void render(CallbackInfo info) {
        TextDisplay.State.STATES.forEach(TextDisplay.State::maybeUpdatePosition);
    }

}
