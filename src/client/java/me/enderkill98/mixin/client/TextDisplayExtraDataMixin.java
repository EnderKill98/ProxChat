package me.enderkill98.mixin.client;

import me.enderkill98.TextDisplayExtraData;
import net.minecraft.entity.decoration.DisplayEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = { DisplayEntity.TextDisplayEntity.class })
public class TextDisplayExtraDataMixin implements TextDisplayExtraData {

    @Unique public float proxChat$minWidth = 0f;

    @Override
    public float proxChat$getMinWidth() {
        return proxChat$minWidth;
    }

    @Override
    public void proxChat$setMinWidth(float minWidth) {
        proxChat$minWidth = minWidth;
    }
}
