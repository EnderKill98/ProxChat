package me.enderkill98.proxchat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.enderkill98.proxchat.ProxChatMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MidTickMixin {

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;handleInputEvents()V"))
    public void afterRunningHandleInputEvents(CallbackInfo info) {
        ProxChatMod.onMidTick();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;tick()V"))
    public void beforeHandlingGameTick(GameRenderer instance, Operation<Void> original) {
        // Run close to handleInputEvents, which send Interact packets
        ProxChatMod.onMidTick();
        original.call(instance);
    }

}
