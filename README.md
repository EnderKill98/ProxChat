# ProxChat

Another madness of mine. Found it randomly when fooling around with someone else: You receive all Block nearby Break actions with no filtering.

This Mod abuses this fact and sends Block Break Aborts around the player. Data is encoded as the relative positions. This allows currently for 9 bits + some additional values per packet.

When this Mod is installed you can type "% Hello World!" in chat, and will send that message to nearby players.

No idea where this should go, but I added some basic packet format to allow for inter-compatible stuff. The code can be used like a library for other purposes.

## How to receive stuff

This used to be done with some rather over-the-top Mixin stuff, because I was lazy. You can still do that and old mixins should not break.

But for convenience and shorter code, I added a crude API.

Essentially you wanna make sure that your mod `suggests` or `depends` on ProxChat, so ProxChat is loaded before your mod.

Then in your mods Initialize-Method, add something like this:

```java
ProxyChatMod.addGlobalHandler((sender, id, data) -> {
    // Check for and handle packets...
});
```

### Choosing your own packet id

I'd propose that the short is split into 2 bytes: Mod Byte, Packet Id byte.

ProxChat uses packets starting from 0, which implies that the "Mod Byte" is 0.

If you want to add your own packets and make sure they won't potentially collide with anyone else:

```java
byte MOD_BYTE = 87; // Chosen with some random number generator
short PACKET_FIRST_FEATURE = (MOD_BYTE << 8) | 0;
short PACKET_SECOND_FEATURE = (MOD_BYTE << 8) | 1;
short PACKET_THIRD_FEATURE = (MOD_BYTE << 8) | 2;
// And so on
```

This essentially gives you 256 packet ids for yourself which are easily numbered, in your control and unlikely to collide with anyone else.

## How to send stuff

ProxChatMod contains a sendPacket method to easily send data.

As mentioned above, you can define your own id and just put whatever bytes you want in. The Id and length/content of the byte buffer are taken care for you and the function used in the example also adds the proper magic so other players know a packet is starting.

Usually malformed packets will fail to be received due to faulty length and received data. But it is possible to receive malformed bytes. So you might want to consider adding a simple checksum or hash to your own packet to verify integrity. But I currently don't see a need for this.
