# ProxChat

Another madness of mine. Found it randomly when fooling around with someone else: You receive all Block nearby Break actions with no filtering.

This Mod abuses this fact and sends data is Block Break Aborts around the player. Data is encoded as the relative positions. This allows currently for 9 bits + some additional values per packet.

When this Mod is installed you can type "% Hello World!" in chat, and will send that message to nearby players.

No idea where this should go, but I added some basic packet format to allow for inter-compatible stuff. The code can be used like a library for other purposes.