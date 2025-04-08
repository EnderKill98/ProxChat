package me.enderkill98.proxchat.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.enderkill98.proxchat.TextDisplayExtraData;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.client.render.entity.state.TextDisplayEntityRenderState;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public abstract class TextDisplayRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;F)V", at = @At("RETURN"))
    public void updateRenderState(DisplayEntity.TextDisplayEntity entity, TextDisplayEntityRenderState state, float f, CallbackInfo info) {
        // Transfer extra data from entity to render state
        ((TextDisplayExtraData) state).proxChat$setMinWidth(((TextDisplayExtraData) entity).proxChat$getMinWidth());
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/TextDisplayEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;width()I"))
    public int getTextWidthFromRender(DisplayEntity.TextDisplayEntity.TextLines instance, Operation<Integer> original, @Local(argsOnly = true) TextDisplayEntityRenderState state) {
        return Math.max(original.call(instance), (int) ( ((TextDisplayExtraData) state).proxChat$getMinWidth() * 40 ));
    }

}
