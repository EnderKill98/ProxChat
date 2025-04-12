package me.enderkill98.proxchat;

import com.aayushatharva.brotli4j.encoder.Encoder;
import me.enderkill98.proxlib.ProxDataUnits;
import me.enderkill98.proxlib.ProxPackets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Packets {
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
        for (int pdu : ProxPackets.PACKET_PDU_MAGIC)
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
                ProxyChatMod.LOGGER.warn("Failed to parse received Emotecraft Packet. No action found for actionId " + actionId);
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
