package me.enderkill98.proxchat;

import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.NameableEnum;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class Config {

    public enum EmotecraftEnableState implements NameableEnum {
        IfMissingOnlineEmotes,
        Yes,
        No;

        @Override
        public Text getDisplayName() {
            return switch (this) {
                case IfMissingOnlineEmotes -> Text.literal("§6If missing OnlineEmotes");
                case Yes -> Text.literal("§aYes");
                case No -> Text.literal("§cNo");
            };
        }

        public boolean should() {
            return switch (this) {
                case No -> false;
                case Yes -> true;
                case IfMissingOnlineEmotes -> !ProxChatMod.hasOnlineEmotes;
            };
        }
    }

    public enum DisplayLegacyPats implements NameableEnum {
        Deduplicated,
        Yes,
        No;

        @Override
        public Text getDisplayName() {
            return switch(this) {
                case Deduplicated -> Text.literal("§6Deduplicated");
                case Yes -> Text.literal("§aYes");
                case No -> Text.literal("§cNo");
            };
        }

        public boolean should(UUID sender) {
            return switch (this) {
                case No -> false;
                case Yes -> true;
                case Deduplicated -> !ProxChatMod.ignoreLegacyPatsFrom.contains(sender);
            };
        }
    }

    public static ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(Identifier.of("proxchat", "config"))
                    .serializer(config -> GsonConfigSerializerBuilder.create(config)
                            .setPath(FabricLoader.getInstance().getConfigDir().resolve("proxchat.json5"))
                            .appendGsonBuilder(GsonBuilder::setPrettyPrinting) // not needed, pretty print by default
                            .setJson5(true)
                            .build())
                    .build();

    @SerialEntry(comment = "Whether to send legacy pat packets.")
    public boolean sendLegacyPats = false;

    @SerialEntry(comment = "Whether to display received legacy pats packets.")
    public DisplayLegacyPats displayLegacyPats = DisplayLegacyPats.Deduplicated;

    @SerialEntry(comment = "Whether to send emote packets.")
    public EmotecraftEnableState sendEmotes = EmotecraftEnableState.IfMissingOnlineEmotes;

    @SerialEntry(comment = "Whether to display received emote packets.")
    public EmotecraftEnableState displayEmotes = EmotecraftEnableState.Yes;

    /**
     * If config changes too much, values can become null and crash on showing the config screen
     */
    public void nullsToDefault() {
        if(displayLegacyPats == null) displayLegacyPats = HANDLER.defaults().displayLegacyPats;
        if(sendEmotes == null) sendEmotes = HANDLER.defaults().sendEmotes;
        if(displayEmotes == null) displayEmotes = HANDLER.defaults().displayEmotes;
    }

}
