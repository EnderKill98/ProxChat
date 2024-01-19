# ProxChat

Another madness of mine. Found it randomly when fooling around with someone else: You receive all Block nearby Break actions with no filtering.

This Mod abuses this fact and sends Block Break Aborts around the player. Data is encoded as the relative positions. This allows currently for 9 bits + some additional values per packet.

When this Mod is installed you can type "% Hello World!" in chat, and will send that message to nearby players.

No idea where this should go, but I added some basic packet format to allow for inter-compatible stuff. The code can be used like a library for other purposes.

## How to receive stuff

You can use it as a library. Either copy the code or include the mod as a lib.

If you don't want to replicate the parsing of other players packets, you can hook into the created handler using a mixin and add your own like this:

`build.gradle`:

```groovy
dependencies {
    modApi fileTree(dir: "../ModJars/MC1.20.4", include: "*.jar") // Mods will get copied and run in runClient
    //modCompileOnlyApi fileTree(dir: "../ModJars/MC1.20.2", include: "*.jar") // Don't include in runClient
}
```

(I should probably make the mod export a maven package which you could include instead easily. For now there is only a file.)

Add this to your `mixins.json`:

```json
{
  "plugin": "your.package.modid.MixinPlugin"
}
```

Which points to this class which can now decide what mixin to load.  
`MixinPlugin.java`:

```java
package me.enderkill98.hello_fabric;

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
```

And then you can simply add mixins into a sub-package. E.g. if a mixin class for you has the path `your.package.modid.mixins.ExampleMixin`, it would become like this to only load if the corresponding mod (by modid) is loaded: `your.package.modid.mixins.mods.proxchat.ExampleMixin`

Also make sure to add `proxchat` as a `suggests` or `depends` so it gets loaded before your mod is.

Afterwards add a mixin in your `mixins.mods.proxchat` package similar like this:

```java
package your.package.modid.mixin.mods.proxchat;

@Mixin(ProxyChatMod.class)
public class ProxPacketReadMixin {
    @Inject(at = @At("TAIL"), method = "addHandlers", remap = false)
    private static void addHandlers(ProxFormat.ProxPlayerReader reader, CallbackInfo info) {
        reader.addHandler((id, data) -> {
            if (id != ProxFormat.ProxPackets.PACKET_ID_CHAT) return;
            String message = ProxFormat.ProxPackets.readChatPacket(data);
            String senderUserName = receiver.getPlayer().getGameProfile().getName();
            YourMod.LOGGER.info("Received a chat packet from " + senderUserName + ": " + message);
        });
    }
}
```

You can for example choose your own Id to respond to for parsing packets. Ids are a 16 bit short. So best you choose some random offset and add numbers to it to make sure yours don't collide with others.

## How to send stuff

See this class to get an example on how to send your own packets or the existing Chat packet: [SendMessageMixin.java](https://github.com/EnderKill98/ProxChat/blob/main/src/client/java/me/enderkill98/mixin/client/SendMessageMixin.java)

As mentioned above, you can define your own id and just put whatever bytes you want in. The Id and length/content of the byte buffer are taken care for you and the function used in the example also adds the proper magic so other players know a packet is starting.

Usually malformed packets will fail to be received due to faulty length and received data. But it is possible to receive malformed bytes. So you might want to consider adding a simple checksum or hash to your own packet to verify integrity.
