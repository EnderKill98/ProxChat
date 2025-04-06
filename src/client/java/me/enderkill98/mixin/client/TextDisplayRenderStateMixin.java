package me.enderkill98.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import me.enderkill98.TextDisplayExtraData;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public abstract class TextDisplayRenderStateMixin {

    @WrapOperation(method = "render(Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity;Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$Data;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IF)V",
    at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/DisplayEntity$TextDisplayEntity$TextLines;width()I"))
    public int getTextWidthFromRender(DisplayEntity.TextDisplayEntity.TextLines instance, Operation<Integer> original, @Local(argsOnly = true) DisplayEntity.TextDisplayEntity textDisplayEntity) {
        return Math.max(original.call(instance), (int) ( ((TextDisplayExtraData) textDisplayEntity).proxChat$getMinWidth() * 40 ));
    }

}
