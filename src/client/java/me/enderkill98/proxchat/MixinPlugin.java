package me.enderkill98.proxchat;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class MixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if(mixinClassName.contains(".mods.")) {
            String[] packageDirs = mixinClassName.split("\\.");
            String modId = null;
            for(int i = 0; i < packageDirs.length; i++)
                if(packageDirs[i].equals("mods"))
                    modId = packageDirs[i+1];
            if(modId == null) return true;
            //boolean modLoaded = FabricLoader.getInstance().isModLoaded(modId);
            // DO NOT use logger of your mod here. That kinda loads your mod twice and causes all sorts of problems!
            //System.out.println("Mixin \"" + mixinClassName + "\" is conditional for mod \"" + modId + "\": " + (modLoaded ? "Should load" : "Should NOT load"));
            return FabricLoader.getInstance().isModLoaded(modId);
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
