package me.enderkill98.proxchat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    private Text text(String text) {
        return Text.literal(text);
    }

    private OptionDescription textOptDesc(String text) {
        return OptionDescription.of(Text.literal(text));
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parentScreen -> YetAnotherConfigLib.createBuilder()
                .title(Text.literal("ProxChat"))
                .category(ConfigCategory.createBuilder()
                        .name(text("ProxChat"))
                        .group(OptionGroup.createBuilder()
                                .name(text("PatPat-Integration (Legacy)"))
                                .description(textOptDesc("""
                                        This is the old/original implementation to send Pats via ProxChat/ProxLib to nearby players regardless of server integration.
                                        
                                        PatPat includes native ProxLib support since 1.2.0 (needs to be enabled in PatPat Mod Settings) which obsoletes this integration.
                                        
                                        This integration is merely kept for legacy support of older ProxChat users (so they can see your pats and you can see theirs).
                                        """))
                                .option(Option.<Boolean>createBuilder()
                                        .name(text("Send Legacy Pats"))
                                        .description(textOptDesc("It is recommended to §lnot§r send Legacy Pats anymore, except if you know a friend has not updated to PatPat 1.2.0+ yet."))
                                        .controller((opt) -> BooleanControllerBuilder.create(opt).formatValue((val) -> text(val ? "Yes" : "No")).coloured(true))
                                        .binding(Config.HANDLER.defaults().sendLegacyPats, () -> Config.HANDLER.instance().sendLegacyPats, (newVal) -> Config.HANDLER.instance().sendLegacyPats = newVal)
                                        .build()
                                )
                                .option(Option.<Config.DisplayLegacyPats>createBuilder()
                                        .name(text("Display Legacy Pats"))
                                        .description(textOptDesc("""
                                                    This is usually fine to keep on, so you can see Pats from people with older PatPat (< 1.2.0), that use ProxChat as well.
                                                    
                                                    Double-pats should be automatically filtered in Deduplicate mode. If you still notice them occasionally or want to check whether someones Pats are from the newer PatPat mod or this integration, you can turn this off.
                                                    
                                                    Another source of double pats will be playing on a server where PatPat is also installed server side. So turn it off in this case as well.
                                                    """))
                                        .controller((opt) -> EnumControllerBuilder.create(opt).enumClass(Config.DisplayLegacyPats.class))
                                        .binding(Config.HANDLER.defaults().displayLegacyPats, () -> Config.HANDLER.instance().displayLegacyPats, (newVal) -> Config.HANDLER.instance().displayLegacyPats = newVal)
                                        .build()
                                )
                                .option(ButtonOption.createBuilder()
                                        .name(text("Clear Deduplication Cache"))
                                        .description(textOptDesc("Forgets who was sending duplicated pats (since this client started)."))
                                        .action((_screen, _opt) -> ProxChatMod.clearPatDeduplicationCache())
                                        .build()
                                )

                                .build()
                        )

                        .group(OptionGroup.createBuilder()
                                .name(text("Emotecraft-Integration"))
                                .description(textOptDesc("""
                                        This sends emotes from Emotecraft. Others can see you emotes if they have Emotecraft, ProxChat and the exact same emote.
                                        
                                        Note that Emotecraft has their own Mod, §lOnline Emotes§r that does a very similar thing.
                                        Their solution has the advantage of being first party (better support) e.g. seeing emotes you don't have,
                                        at the drawback of needing a third party server to send the additional emote data over.
                                        """))
                                .option(Option.<Config.EmotecraftEnableState>createBuilder()
                                        .name(text("Send Emotes"))
                                        .description(textOptDesc("Whether ProxChat should send packet if you Emoting with Emotecraft."))
                                        .controller((opt) -> EnumControllerBuilder.create(opt).enumClass(Config.EmotecraftEnableState.class))
                                        .binding(Config.HANDLER.defaults().sendEmotes, () -> Config.HANDLER.instance().sendEmotes, (newVal) -> Config.HANDLER.instance().sendEmotes = newVal)
                                        .build()
                                )
                                .option(Option.<Config.EmotecraftEnableState>createBuilder()
                                        .name(text("Display Emotes"))
                                        .description(textOptDesc("""
                                                Whether ProxChat should display Emotecraft Emotes that were received as ProxChat packets.
                                                
                                                If you notice issues with Emote-Play back (double-emoting, jitter, other weirdness), change this to "Never".
                                                """))
                                        .controller((opt) -> EnumControllerBuilder.create(opt).enumClass(Config.EmotecraftEnableState.class))
                                        .binding(Config.HANDLER.defaults().displayEmotes, () -> Config.HANDLER.instance().displayEmotes, (newVal) -> Config.HANDLER.instance().displayEmotes = newVal)
                                        .build()
                                )
                                .build()
                        )
                        .build()
                )
                .save(() -> Config.HANDLER.save())
                .build()
                .generateScreen(parentScreen);
    }

}
