package me.enderkill98;

import com.aayushatharva.brotli4j.encoder.Encoder;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.*;
import java.util.*;

public class ProxFormat {
    public static final Logger LOGGER = ProxyChatMod.LOGGER;

    // ProxDataUnit is an Integer!

    public static class ProxDataUnits {

        private static BlockPos[] createAllOffsets() {
            // All tested offsets have to be in reach of all these positions
            final ArrayList<Vec3d> originPositions = new ArrayList<>();
            for (int xOffset : new int[]{0, 1})
                for (int yOffset : new int[]{0, 1})
                    for (int zOffset : new int[]{0, 1})
                        originPositions.add(new Vec3d(xOffset, yOffset, zOffset));

            // Find all offsets that can be interacted with
            final ArrayList<BlockPos> offsets = new ArrayList<>();
            final int[] anyAxisOffsets = new int[]{0, 1, -1, 2, -2, 3, -3, 4, -4, 5, -5, 6, -6};
            for (int x : anyAxisOffsets) {
                for (int y : anyAxisOffsets) {
                    offsetLoop:
                    for (int z : anyAxisOffsets) {
                        BlockPos offset = new BlockPos(x, y, z);
                        Vec3d offsetCenter = Vec3d.ofCenter(offset);
                        for (Vec3d originPos : originPositions)
                            if (originPos.squaredDistanceTo(offsetCenter) > 6.0 * 6.0)
                                continue offsetLoop; // Too far to interact with

                        offsets.add(offset);
                    }
                }
            }

            return offsets.toArray(new BlockPos[0]);
        }

        public static final BlockPos[] ALL_OFFSETS = createAllOffsets();

        private static HashMap<BlockPos, Integer> createOffsetLookupMap() {
            HashMap<BlockPos, Integer> lookupMap = new HashMap<>();
            for (int offsetIndex = 0; offsetIndex < ALL_OFFSETS.length; offsetIndex++) {
                lookupMap.put(ALL_OFFSETS[offsetIndex], offsetIndex);
            }
            return lookupMap;
        }

        public static final HashMap<BlockPos, Integer> ALL_OFFSETS_LOOKUP_MAP = createOffsetLookupMap();

        public static int getStorableBitCount() {
            if (ALL_OFFSETS.length == 0) return 0;
            return (int) Math.floor(Math.log(ALL_OFFSETS.length) / Math.log(2));
        }

        // Value is exclusive
        public static int getMaxUsableProxDataUnit() {
            return (int) Math.pow(2, getStorableBitCount());
        }

        // Value is exclusive
        public static int getMaxProxDataUnit() {
            return ALL_OFFSETS.length;
        }

        public static List<Integer> bytesToProxDataUnits(byte[] input) {
            if (input.length == 0) return Collections.emptyList();

            ArrayList<Integer> output = new ArrayList<>();
            int highestBit = getStorableBitCount();

            int currentOffsetIndex = 0;
            int currentOffsetIndexPos = 0;
            for (byte inputByte : input) {
                for (int i = 0; i < 8; i++) {
                    int inputBit = (inputByte >>> i) & 0x01; // 0 or 1

                    currentOffsetIndex |= (inputBit << currentOffsetIndexPos);
                    currentOffsetIndexPos++;
                    if (currentOffsetIndexPos == highestBit) {
                        output.add(currentOffsetIndex);
                        currentOffsetIndex = 0;
                        currentOffsetIndexPos = 0;
                    }
                }
            }
            if (currentOffsetIndexPos > 0)
                output.add(currentOffsetIndex);

            return output;
        }

        public static byte[] proxDataUnitsToBytes(int... inputProxDataUnits) {
            ProxDataUnitReader reader = new ProxDataUnitReader();
            for (int proxDataUnit : inputProxDataUnits)
                reader.read(proxDataUnit);
            return reader.getBytes();
        }

        public static BlockPos proxDataUnitToBlockPos(PlayerEntity player, int offsetIndex) {
            final Vec3d eyePos = player.getEyePos();
            final BlockPos eyeBlockPos = new BlockPos(MathHelper.floor(eyePos.getX()), MathHelper.floor(eyePos.getY()), MathHelper.floor(eyePos.getZ()));
            return eyeBlockPos.add(ALL_OFFSETS[offsetIndex]);
        }

        public static int blockPosToProxDataUnit(PlayerEntity player, BlockPos blockPos) {
            final Vec3d eyePos = player.getEyePos();
            final BlockPos eyeBlockPos = new BlockPos(MathHelper.floor(eyePos.getX()), MathHelper.floor(eyePos.getY()), MathHelper.floor(eyePos.getZ()));

            BlockPos offset = blockPos.subtract(eyeBlockPos);
            return ALL_OFFSETS_LOOKUP_MAP.getOrDefault(offset, -1);
        }

        public static int blockPosToProxDataUnit(BlockPos eyeBlockPos, BlockPos blockPos) {
            BlockPos offset = blockPos.subtract(eyeBlockPos);
            return ALL_OFFSETS_LOOKUP_MAP.getOrDefault(offset, -1);
        }
    }

    public static class ProxDataUnitReader {
        private static final int HIGHEST_IN_BIT = ProxDataUnits.getStorableBitCount();
        private static final int HIGHEST_OUT_BIT = 8;

        private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        private byte currentByte = 0;
        private int currentBytePos = 0;

        public void read(int proxDataUnit) {
            for (int i = 0; i < HIGHEST_IN_BIT; i++) {
                int inputBit = (proxDataUnit >>> i) & 0x01; // 0 or 1

                currentByte |= (byte) (inputBit << currentBytePos);
                currentBytePos++;
                if (currentBytePos == HIGHEST_OUT_BIT) {
                    outputStream.write(currentByte);
                    currentByte = 0;
                    currentBytePos = 0;
                }
            }
        }

        public int getTotalBytes() {
            return outputStream.size();
        }

        public byte[] getBytes() {
            return outputStream.toByteArray();
        }

    }

    public static class ProxPackets {
        public static int[] PACKET_PDU_MAGIC = new int[]{ProxDataUnits.getMaxProxDataUnit() - 1, ProxDataUnits.getMaxProxDataUnit() - 19};

        public static short PACKET_ID_CHAT = 1;
        public static short PACKET_ID_PATPAT_PATENTITY = 2;
        public static short PACKET_ID_EMOTECRAFT = 3;
        public static short PACKET_ID_TEXTDISPLAY = 4;

        public interface ProxPacketReceiveHandler {
            void onReceived(short id, byte[] data);
        }

        // This is still missing the MAGIC, which can't be encoded here
        public static byte[] encodeProxPacket(short id, byte[] data) {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            int length = 2 /*Id*/ + data.length;

            // Write length
            bout.write((byte) ((length >> 16) & 0xFF));
            bout.write((byte) ((length >> 8) & 0xFF));
            bout.write((byte) (length & 0xFF));

            // Write id
            bout.write((byte) ((id >> 8) & 0xFF));
            bout.write((byte) (id & 0xFF));

            // Write data
            try {
                bout.write(data);
            } catch (IOException ex) {
                return null; // Should never happen tbh
            }

            return bout.toByteArray();
        }

        public static List<Integer> fullyEncodeProxPacketToProxDataUnits(short id, byte[] data) {
            ArrayList<Integer> pdus = new ArrayList<>();
            for (int pdu : PACKET_PDU_MAGIC)
                pdus.add(pdu);

            byte[] encoded = encodeProxPacket(id, data);
            pdus.addAll(ProxDataUnits.bytesToProxDataUnits(encoded));
            return pdus;
        }

        public static byte[] createChatPacket(String message) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeUTF(message);
                dout.close();
                byte[] data = bout.toByteArray();
                //LOGGER.info("Created chat packet which has " + data.length + " bytes: " + new String(Hex.encodeHex(data)));
                return data;
            } catch (IOException ex) {
                return null;
            }
        }

        public static String readChatPacket(byte[] data) {
            //LOGGER.info("Attempting to read chat packet which has " + data.length + " bytes: " + new String(Hex.encodeHex(data)));
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(data);
                DataInput din = new DataInputStream(bin);
                String message = din.readUTF();
                bin.close();
                return message;
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public static byte[] createPatPatPatEntityPacket(int pattedEntityId) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeInt(pattedEntityId);
                dout.close();
                return bout.toByteArray();
            } catch (IOException ex) {
                return null;
            }
        }

        public static int readPatPatPatEntityPacket(byte[] data) {
            //LOGGER.info("Attempting to read chat packet which has " + data.length + " bytes: " + new String(Hex.encodeHex(data)));
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(data);
                DataInput din = new DataInputStream(bin);
                int pattedEntityId = din.readInt();
                bin.close();
                return pattedEntityId;
            } catch (IOException ex) {
                ex.printStackTrace();
                return -1;
            }
        }

        public static enum EmotecraftAction {
            StartEmote,
            RepeatEmote,
            StopEmote,
        }

        public static record EmotecraftData(@NotNull EmotecraftAction action, @Nullable UUID emoteUuid, int tick) {}

        public static byte[] createEmotecraftPacket(EmotecraftData ecData) {
            try {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeByte(ecData.action.ordinal());
                dout.writeBoolean(ecData.emoteUuid != null); // Has EmoteUUID
                if(ecData.emoteUuid != null) {
                    dout.writeLong(ecData.emoteUuid.getMostSignificantBits());
                    dout.writeLong(ecData.emoteUuid.getLeastSignificantBits());
                }
                dout.writeInt(ecData.tick);
                dout.close();
                return bout.toByteArray();
            } catch (IOException ex) {
                return null;
            }
        }

        public static EmotecraftData readEmotecraftPacket(byte[] data) {
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(data);
                DataInput din = new DataInputStream(bin);
                EmotecraftAction action = null;
                int actionId = din.readByte();
                for(EmotecraftAction maybeAction : EmotecraftAction.values()) {
                    if(maybeAction.ordinal() == actionId) {
                        action = maybeAction;
                        break;
                    }
                }
                if(action == null) {
                    ProxFormat.LOGGER.warn("Failed to parse received Emotecraft Packet. No action found for actionId " + actionId);
                    return null;
                }

                boolean hasEmoteUuid = din.readBoolean();
                UUID emoteUuid = null;
                if(hasEmoteUuid) {
                    long mostSiginificantBits = din.readLong();
                    long leastSiginificantBits = din.readLong();
                    emoteUuid = new UUID(mostSiginificantBits, leastSiginificantBits);
                }

                int tick = din.readInt();
                bin.close();
                return new EmotecraftData(action, emoteUuid, tick);
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public static byte[] createTextDisplayPacket(TextDisplay.TextDisplayPacket packet, boolean includeSize, @Nullable Encoder.Mode brotliEncoderMode) {
            try {
                return packet.encode(includeSize, brotliEncoderMode);
            }catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public static TextDisplay.TextDisplayPacket readTextDisplayPacket(byte[] data) {
            try {
                return TextDisplay.TextDisplayPacket.decode(data);
            }catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public static class ProxPlayerReader {
        private PlayerEntity player;
        private int magicBytesPos = 0;
        private @Nullable BlockPos assumedPlayerEyeBlockPos = null;
        private ProxDataUnitReader dataReader = null;
        private Pair<Integer/*Length (3 byte)*/, @Nullable Short/*Id (2 byte)*/> dataHeader = null;
        private long lastReceivedAt = -1L;
        private ArrayList<ProxPackets.ProxPacketReceiveHandler> handlers = new ArrayList<>();

        public ProxPlayerReader(PlayerEntity player) {
            this.player = player;
        }

        public PlayerEntity getPlayer() {
            return player;
        }

        public void addHandler(ProxPackets.ProxPacketReceiveHandler handler) {
            handlers.add(handler);
        }

        public void handle(BlockBreakingProgressS2CPacket packet) {
            final long now = System.currentTimeMillis();
            if (lastReceivedAt != -1L && now - lastReceivedAt > 3000) {
                magicBytesPos = 0;
                assumedPlayerEyeBlockPos = null;
                dataReader = null;
                dataHeader = null;
            }
            lastReceivedAt = now;

            if (packet.getProgress() != 255) return; // We only care about ABORT_BLOCK_BREAKs
            if (packet.getEntityId() != player.getId()) return; // Not for this player

            // Mid-MagicByte parsing
            if (dataReader == null && assumedPlayerEyeBlockPos != null && magicBytesPos > 0 && magicBytesPos < ProxPackets.PACKET_PDU_MAGIC.length) {
                // Currently, with a 2 PDU-long magic, this will always mean magicBytePos == 1
                int expectedPdu = ProxPackets.PACKET_PDU_MAGIC[magicBytesPos];
                int actualPdu = ProxDataUnits.blockPosToProxDataUnit(assumedPlayerEyeBlockPos, packet.getPos());
                //LOGGER.info("Mid-MagicBytes pos: " + magicBytesPos + ", expected PDU: " + expectedPdu + ", actual PDU: " + actualPdu);
                if (expectedPdu == actualPdu) {
                    magicBytesPos++;
                    if (magicBytesPos == ProxPackets.PACKET_PDU_MAGIC.length) {
                        // Full magic received!
                        dataReader = new ProxDataUnitReader();
                        dataHeader = null;
                        return; // Do not process this as a data pdu later on
                    }
                } else {
                    // Reset
                    magicBytesPos = 0;
                    assumedPlayerEyeBlockPos = null;
                }
            }

            // Start MagicByte parsing
            if(dataReader == null && magicBytesPos == 0) {
                BlockPos firstByteOffset = ProxDataUnits.ALL_OFFSETS[ProxPackets.PACKET_PDU_MAGIC[0]];
                // Blindly assume that player sent this byte on purpose and therefore has a corresponding EyeBlockPos
                assumedPlayerEyeBlockPos = packet.getPos().subtract(firstByteOffset);
                magicBytesPos++;
                //LOGGER.info("Start-MagicBytes pos: " + magicBytesPos + ", assumedPlayerEyeBlockPos: " + assumedPlayerEyeBlockPos);
            }

            if(assumedPlayerEyeBlockPos == null || dataReader == null) return; // MagicBytes not successfully received yet

            int pdu = ProxDataUnits.blockPosToProxDataUnit(assumedPlayerEyeBlockPos, packet.getPos());
            //LOGGER.info("Data PDU: " + pdu);
            if(pdu == -1) return; // Invalid offset (maybe player moved too much?)

            if(pdu >= ProxDataUnits.getMaxUsableProxDataUnit()) {
                // Those are meant for use by MagicBytes only. Consider this an error and reset
                dataReader = null;
                dataHeader = null;
                return;
            }

            magicBytesPos = 0;
            if(dataReader == null)
                return; // Not expecting any data rn. Ignoring
            dataReader.read(pdu);

            final int totalBytes = dataReader.getTotalBytes();
            if(totalBytes >= 3 && dataHeader == null) {
                // Got enough data to figure out expected length
                byte[] bytes = dataReader.getBytes();
                // If not "& 0xFF"'ing, any byte with the highest bit in a bight can make the whole Integer negative for some reason!!!!!!!!
                int expectedLength = ((bytes[0] & 0xFF) << 16) | ((bytes[1] & 0xFF) << 8) | (bytes[2] & 0xFF);
                dataHeader = new Pair<>(expectedLength, null);
            }else if(totalBytes >= 5 && dataHeader != null && dataHeader.getRight() == null) {
                // Got enough data to figure out the id
                byte[] bytes = dataReader.getBytes();
                short id = (short) (bytes[3+0] << 8 | bytes[3+1]);
                dataHeader.setRight(id);
            }else if(dataHeader != null && dataHeader.getRight() != null && totalBytes >= 3+dataHeader.getLeft()) {
                // All data got read
                int expectedLength = dataHeader.getLeft();
                @Nullable Short id = dataHeader.getRight();
                if(expectedLength < 2 || id == null) {
                    LOGGER.info("Packet received from " + player.getGameProfile().getName() + " was too small (length was: " + expectedLength + " and id " + id + ")!");
                    dataReader = null;
                    dataHeader = null;
                    return;
                }

                byte[] data = Arrays.copyOfRange(dataReader.getBytes(), 5, expectedLength+3);
                LOGGER.info("Received a prox packet with id " + id + " and " + data.length + " bytes of data.");
                for(ProxPackets.ProxPacketReceiveHandler handler : handlers) {
                    try {
                        handler.onReceived(id, data);
                    }catch (Exception ex) {
                        LOGGER.error("Failed when running some handler!", ex);
                    }
                }

                // Done
                dataReader = null;
                dataHeader = null;
            }
        }
    }

}
