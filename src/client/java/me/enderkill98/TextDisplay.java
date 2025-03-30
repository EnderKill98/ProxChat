package me.enderkill98;

import com.aayushatharva.brotli4j.decoder.BrotliInputStream;
import com.aayushatharva.brotli4j.encoder.BrotliEncoderChannel;
import com.aayushatharva.brotli4j.encoder.Encoder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;

public class TextDisplay {

    public static enum Compression {
        None,
        Brotli;

        public byte getId() {
            return (byte) ordinal();
        }
    }

    public static record TextDisplayPacket(@NotNull Compression compression, @NotNull Command[] commands) {
        public byte[] encode(boolean includeSize) throws IOException {
            if(commands.length >= 256)
                throw new IllegalArgumentException("More than 256 commands in a single packet are not supported!");
            // Encode array of commands with size
            byte[] commandBytes;
            {
                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                DataOutputStream dout = new DataOutputStream(bout);
                dout.writeByte(commands.length);
                for (Command command : commands)
                    command.writeCommand(dout, includeSize);
                commandBytes = bout.toByteArray();
            }

            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            DataOutputStream dout = new DataOutputStream(bout);
            dout.writeByte(compression.getId());
            switch (compression) {
                case None -> {
                    dout.write(commandBytes);
                }
                case Brotli -> {
                    if(!ProxyChatMod.hasBrotli)
                        throw new RuntimeException("Tried to encode a Brotli-Compressed TextDisplayPacket, but the Brotli Library failed to load!");

                    Encoder.Parameters encoderParams = new Encoder.Parameters();
                    encoderParams.setQuality(11);
                    encoderParams.setMode(Encoder.Mode.TEXT);
                    byte[] compressed = BrotliEncoderChannel.compress(commandBytes, encoderParams);
                    if(compressed.length >= 32768)
                        throw new RuntimeException("Encoded packet would have a way too big compressed size!");
                    dout.writeShort(compressed.length);
                    dout.write(compressed);
                }
            }

            return bout.toByteArray();
        }

        public static TextDisplayPacket decode(byte[] encoded) throws IOException {
            DataInputStream din = new DataInputStream(new ByteArrayInputStream(encoded));
            int compressionId = din.readUnsignedByte();
            Compression compression = null;
            for(Compression maybeCompression : Compression.values()) {
                if(maybeCompression.getId() == compressionId) {
                    compression = maybeCompression;
                    break;
                }
            }

            return switch(compression) {
                case None -> new TextDisplayPacket(compression, readCommands(din));
                case Brotli -> {
                    if(!ProxyChatMod.hasBrotli)
                        throw new RuntimeException("Tried to decode a Brotli-Compressed TextDisplayPacket, but the Brotli Library failed to load!");

                    int compressedSize = din.readShort();
                    byte[] compressed = new byte[compressedSize];
                    din.read(compressed);

                    ByteArrayInputStream dataIn = new ByteArrayInputStream(compressed);
                    BrotliInputStream brotIn = new BrotliInputStream(dataIn);
                    final int MAX_DECOMPRESSION_SIZE = 1024 * 10; // Don't allow more than 10 KiB of data (to mitigate "Zip Bombs")
                    ByteArrayOutputStream decompressed = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    int total = 0;
                    while((count = brotIn.read(buffer)) > 0) {
                        total += count;
                        if(total > MAX_DECOMPRESSION_SIZE) {
                            throw new IOException("Decompressed data would have been over the internal limit (" + MAX_DECOMPRESSION_SIZE + " bytes). Decompression was aborted to mitigate \"Zip Bomb\" or other harmful actions to the MC Client.");
                        }

                        decompressed.write(buffer, 0, count);
                    }

                    yield new TextDisplayPacket(compression, readCommands(new DataInputStream(new ByteArrayInputStream(decompressed.toByteArray()))));
                }
            };
        }

        private static Command[] readCommands(DataInputStream in) throws IOException {
            int commandCount = in.readUnsignedByte();
            ArrayList<Command> commands = new ArrayList<>(commandCount);
            for(int i = 0; i < commandCount; i++) commands.add(Command.readCommand(in));
            return commands.toArray(Command[]::new);
        }

        public static void handle(MinecraftClient client, @Nullable Vec3d maybeAnchorPos, PlayerEntity sender, Command... commands) {
            @NotNull Vec3d anchorPos = maybeAnchorPos == null ? Vec3d.of(sender.getBlockPos()) : maybeAnchorPos;
            if(!client.isOnThread()) {
                Vec3d fAnchorPos = anchorPos;
                client.submit(() -> handle(client, fAnchorPos, sender, commands));
                return;
            }

            Vec3d currentAbsPos = null;
            for(Command command : commands) {
                switch (command) {
                    case Command.SetAnchorPos setAnchorPos -> {
                        anchorPos = setAnchorPos.anchorPos;
                        currentAbsPos = anchorPos;
                    }
                    case Command.SetRelativePos setRelativePos -> currentAbsPos = anchorPos.add(setRelativePos.relPos.x, setRelativePos.relPos.y, setRelativePos.relPos.z);
                    default -> {
                        if(currentAbsPos == null) {
                            ProxFormat.LOGGER.warn("Info failed to execute TextDisplay command " + command + " because no SetRelativePos was received before!");
                            continue;
                        }

                        State state = State.find(currentAbsPos);
                        if(state == null) state = State.create(sender.getUuid(), currentAbsPos);
                        state.handle(client.world, sender.getUuid(), command);
                    }
                }
            }
        }
    }

    public record RelativePos(float x, float y, float z) {
        public void validate() {
            if(x < -48 || x > 48) throw new IllegalArgumentException("RelativePos.x must be within -48 to 48, but is " + x);
            if(y < -24 || y > 24) throw new IllegalArgumentException("RelativePos.y must be within -48 to 48, but is " + y);
            if(z < -48 || z > 48) throw new IllegalArgumentException("RelativePos.z must be within -48 to 48, but is " + z);
        }

        public int toInt() {
            validate();
            int xInt = (short) ((x + 48f) * 20f); // 11 bits
            int yInt = (short) ((y + 24f) * 20f); // 10 bits
            int zInt = (short) ((z + 48f) * 20f); // 11 bits
            return (xInt << (11/*zBits*/+10)/*yBits*/) | (yInt << 11/*zBits*/) | zInt;
        }

        public static RelativePos fromInt(int relPosInt) {
            int elevenBitMask = 2048 - 1;
            int tenBitMask = 1024 - 1;

            int xInt = (relPosInt >>> (11/*zBits*/ + 10/*yBits*/)) & elevenBitMask;
            int yInt = (relPosInt >>> 11/*zBits*/) & tenBitMask;
            int zInt = relPosInt & elevenBitMask;
            return new RelativePos((xInt / 20.0f) - 48f, (yInt / 20.0f) - 24f, (zInt / 20.0f) - 48f);
        }

        public static RelativePos fromRelativePos(Vec3d relPos) {
            return new RelativePos((float) relPos.getX(), (float) relPos.getY(), (float) relPos.getZ());
        }

        public static RelativePos fromAbsolutePos(Vec3d anchorPos, Vec3d absPos) {
            return fromRelativePos(absPos.subtract(anchorPos));
        }

        public static RelativePos fromAbsolutePos(PlayerEntity anchorPlayer, Vec3d absPos) {
            return fromAbsolutePos(Vec3d.of(anchorPlayer.getBlockPos()), absPos);
        }
    }

    public sealed interface Command {
        record SetAnchorPos(Vec3d anchorPos) implements Command {
            public static byte getId() { return (byte) 1; }

            public static SetAnchorPos readContent(DataInputStream in) throws IOException {
                return new SetAnchorPos(new Vec3d(in.readDouble(), in.readDouble(), in.readDouble()));
            }

            public void writeContent(DataOutputStream out) throws IOException {
                out.writeDouble(anchorPos.getX());
                out.writeDouble(anchorPos.getY());
                out.writeDouble(anchorPos.getZ());
            }
        }
        record SetRelativePos(RelativePos relPos) implements Command {
            public static byte getId() { return (byte) 2; }

            public static SetRelativePos readContent(DataInputStream in) throws IOException {
                return new SetRelativePos(RelativePos.fromInt(in.readInt()));
            }

            public void writeContent(DataOutputStream out) throws IOException {
                out.writeInt(relPos.toInt());
            }
        }
        record SetText(String text) implements Command {
            public static byte getId() { return (byte) 3; }

            public static SetText readContent(DataInputStream in) throws IOException {
                return new SetText(in.readUTF());
            }

            public void writeContent(DataOutputStream out) throws IOException {
                out.writeUTF(text);
            }
        }
        record SetRemovalTimeout(int removalTimeoutMillis) implements Command {
            public static byte getId() { return (byte) 4; }

            public static SetRemovalTimeout readContent(DataInputStream in) throws IOException {
                int timeoutInt = in.readUnsignedByte();
                int millis = 500 + timeoutInt * 500; // 0.5s to 120s in 0.5s increments
                return new SetRemovalTimeout(millis);
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(removalTimeoutMillis < 500 || removalTimeoutMillis > 120*1000)
                    throw new IllegalArgumentException("Timeout is not in range of 0.5s to 120s: " + this.removalTimeoutMillis);
                out.writeByte((removalTimeoutMillis - 500) / 500);
            }
        }
        record SetBillboardMode(DisplayEntity.BillboardMode mode) implements Command {
            public static byte getId() { return (byte) 5; }

            public static SetBillboardMode readContent(DataInputStream in) throws IOException {
                int modeId = in.readUnsignedByte();
                return new SetBillboardMode(switch(modeId) {
                    case 0 -> DisplayEntity.BillboardMode.FIXED;
                    case 1 -> DisplayEntity.BillboardMode.VERTICAL;
                    case 2 -> DisplayEntity.BillboardMode.HORIZONTAL;
                    case 3 -> DisplayEntity.BillboardMode.CENTER;
                    default -> throw new IllegalStateException("Unexpected billboard modeId: " + modeId);
                });
            }

            public void writeContent(DataOutputStream out) throws IOException {
                int modeId = switch(mode) {
                    case FIXED -> 0;
                    case VERTICAL -> 1;
                    case HORIZONTAL -> 2;
                    case CENTER -> 3;
                    case null -> throw new IllegalArgumentException("Billboard mode may not be null!");
                };
                out.writeByte(modeId);
            }
        }
        record SetDisplayWidth(float displayWidth) implements Command {
            public static byte getId() { return (byte) 6; }

            public static SetDisplayWidth readContent(DataInputStream in) throws IOException {
                int widthInt = in.readUnsignedByte();
                return new SetDisplayWidth((widthInt / 20f));
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(displayWidth < 0 || displayWidth > 255/20f) throw new IllegalArgumentException("DisplayWidth must be between 0 and " + 255/20f);
                out.writeByte((int) (displayWidth * 20));
            }
        }

        record SetDisplayHeight(float displayHeight) implements Command {
            public static byte getId() { return (byte) 7; }

            public static SetDisplayHeight readContent(DataInputStream in) throws IOException {
                int heightInt = in.readUnsignedByte();
                return new SetDisplayHeight((heightInt / 20f));
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(displayHeight < 0 || displayHeight > 255/20f) throw new IllegalArgumentException("DisplayHeight must be between 0 and " + 255/20f);
                out.writeByte((int) (displayHeight * 20));
            }
        }

        record SetDisplayWidthAndHeight(float displayWidth, float displayHeight) implements Command {
            public static byte getId() { return (byte) 8; }

            public static SetDisplayWidthAndHeight readContent(DataInputStream in) throws IOException {
                int widthInt = in.readUnsignedByte();
                int heightInt = in.readUnsignedByte();
                return new SetDisplayWidthAndHeight((widthInt / 20f), (heightInt / 20f));
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(displayWidth < 0 || displayWidth > 255/20f) throw new IllegalArgumentException("DisplayWidth must be between 0 and " + 255/20f);
                if(displayHeight < 0 || displayHeight > 255/20f) throw new IllegalArgumentException("DisplayHeight must be between 0 and " + 255/20f);
                out.writeByte((int) (displayWidth * 20));
                out.writeByte((int) (displayHeight * 20));
            }
        }

        record SetYaw(float yaw) implements Command {
            public static byte getId() { return (byte) 9; }

            public static SetYaw readContent(DataInputStream in) throws IOException {
                int yawUShort = in.readUnsignedShort();
                return new SetYaw((yawUShort / 150f) - 180f);
            }

            public void writeContent(DataOutputStream out) throws IOException {
                float yaw = this.yaw;
                if(yaw > 180) yaw -= 180;
                if(yaw < -180f || yaw > 180f) throw new IllegalArgumentException("Yaw must be between -180 and 180");
                out.writeShort((int) ((yaw + 180f) * 150));
            }
        }

        record SetPitch(float pitch) implements Command {
            public static byte getId() { return (byte) 10; }

            public static SetPitch readContent(DataInputStream in) throws IOException {
                int pitchUShort = in.readUnsignedShort();
                return new SetPitch((pitchUShort / 300f) - 90f);
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(pitch < -90f || pitch > 90f) throw new IllegalArgumentException("Pitch must be between -90 and 90");
                out.writeShort((int) ((pitch + 90f) * 300));
            }
        }

        record SetYawAndPitch(float yaw, float pitch) implements Command {
            public static byte getId() { return (byte) 11; }

            public static SetYawAndPitch readContent(DataInputStream in) throws IOException {
                int yawUShort = in.readUnsignedShort();
                int pitchUShort = in.readUnsignedShort();
                return new SetYawAndPitch((yawUShort / 150f) - 180f, (pitchUShort / 300f) - 90f);
            }

            public void writeContent(DataOutputStream out) throws IOException {
                float yaw = this.yaw;
                if(yaw > 180) yaw -= 180;
                if(yaw < -180f || yaw > 180f) throw new IllegalArgumentException("Yaw must be between -180 and 180");
                if(pitch < -90f || pitch > 90f) throw new IllegalArgumentException("Pitch must be between -90 and 90");
                out.writeShort((int) ((yaw + 180f) * 150));
                out.writeShort((int) ((pitch + 90f) * 300));
            }
        }

        record SetViewDistance(float viewDistance) implements Command {
            public static byte getId() { return (byte) 12; }

            public static SetViewDistance readContent(DataInputStream in) throws IOException {
                return new SetViewDistance((float) in.readUnsignedByte());
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(viewDistance < 0 || viewDistance > 255f) throw new IllegalArgumentException("ViewDistance must be between 0 and 255");
                out.writeByte((int) viewDistance);
            }
        }

        record SetBackground(int backgroundArgb) implements Command {
            public static byte getId() { return (byte) 13; }

            public static SetBackground readContent(DataInputStream in) throws IOException {
                return new SetBackground(in.readInt());
            }

            public void writeContent(DataOutputStream out) throws IOException {
                out.writeInt(backgroundArgb);
            }
        }

        record SetTextOpacity(byte textOpacity) implements Command {
            public static byte getId() { return (byte) 14; }

            public static SetTextOpacity readContent(DataInputStream in) throws IOException {
                return new SetTextOpacity(in.readByte());
            }

            public void writeContent(DataOutputStream out) throws IOException {
                out.write(new byte[] { textOpacity });
            }
        }

        record SetDisplayFlags(boolean shadow, boolean seeThrough, boolean defaultBackground, DisplayEntity.TextDisplayEntity.TextAlignment textAlignment) implements Command {

            public static byte getId() { return (byte) 16; }

            public static SetDisplayFlags fromFlags(byte flags) {
                boolean shadow = (flags & 1) != 0;
                boolean seeThrough = (flags & 2) != 0;
                boolean defaultBackground = (flags & 4) != 0;
                DisplayEntity.TextDisplayEntity.TextAlignment textAlignment = DisplayEntity.TextDisplayEntity.TextAlignment.CENTER;
                if((flags & 8) != 0)
                    textAlignment = DisplayEntity.TextDisplayEntity.TextAlignment.LEFT;
                if((flags & 16) != 0)
                    textAlignment = DisplayEntity.TextDisplayEntity.TextAlignment.RIGHT;

                return new SetDisplayFlags(shadow, seeThrough, defaultBackground, textAlignment);
            }

            public byte toFlags() {
                byte flags = 0;
                if(shadow) flags |= 1;
                if(seeThrough) flags |= 2;
                if(defaultBackground) flags |= 4;
                if(textAlignment == DisplayEntity.TextDisplayEntity.TextAlignment.LEFT) flags |= 8;
                if(textAlignment == DisplayEntity.TextDisplayEntity.TextAlignment.RIGHT) flags |= 16;
                return flags;
            }

            public static SetDisplayFlags readContent(DataInputStream in) throws IOException {
                return fromFlags(in.readByte());
            }

            public void writeContent(DataOutputStream out) throws IOException {
                out.write(new byte[]{ toFlags() });
            }
        }

        record SetLineWidth(int lineWidth) implements Command {
            public static byte getId() { return (byte) 17; }

            public static SetLineWidth readContent(DataInputStream in) throws IOException {
                return new SetLineWidth(in.readUnsignedByte());
            }

            public void writeContent(DataOutputStream out) throws IOException {
                if(lineWidth < 0 || lineWidth > 255) throw new IllegalArgumentException("LineWidth is not between 0 and 255: " + lineWidth);
                out.writeByte(lineWidth);
            }
        }

        void writeContent(DataOutputStream out) throws IOException;

        public static @Nullable Command readCommand(DataInputStream in) throws IOException, IllegalArgumentException {
            int id = in.readUnsignedByte();
            boolean hasSize = (id & 128) != 0; // When used, limits size to 256 bytes, but allows adding potentially unsupported commands safely
            @Nullable Integer size = hasSize ? in.readUnsignedByte() : null;

            if (id == SetAnchorPos.getId()) return SetAnchorPos.readContent(in);
            else if (id == SetRelativePos.getId()) return SetRelativePos.readContent(in);
            else if (id == SetText.getId()) return SetText.readContent(in);
            else if (id == SetRemovalTimeout.getId()) return SetRemovalTimeout.readContent(in);
            else if (id == SetBillboardMode.getId()) return SetBillboardMode.readContent(in);
            else if (id == SetDisplayWidth.getId()) return SetDisplayWidth.readContent(in);
            else if (id == SetDisplayHeight.getId()) return SetDisplayHeight.readContent(in);
            else if (id == SetDisplayWidthAndHeight.getId()) return SetDisplayWidthAndHeight.readContent(in);
            else if (id == SetYaw.getId()) return SetYaw.readContent(in);
            else if (id == SetPitch.getId()) return SetPitch.readContent(in);
            else if (id == SetYawAndPitch.getId()) return SetYawAndPitch.readContent(in);
            else if (id == SetViewDistance.getId()) return SetViewDistance.readContent(in);
            else if (id == SetBackground.getId()) return SetBackground.readContent(in);
            else if (id == SetTextOpacity.getId()) return SetTextOpacity.readContent(in);
            else if (id == SetDisplayFlags.getId()) return SetDisplayFlags.readContent(in);
            else if (id == SetLineWidth.getId()) return SetLineWidth.readContent(in);
            else {
                if(hasSize) {
                    byte[] throwaway = new byte[size];
                    in.read(throwaway);
                    //ProxFormat.LOGGER.warn("Read an unsupported TextDisplay Command (id " + id + "), which was discarded.");
                    return null;
                }else {
                    throw new IllegalArgumentException("Unknown Command Id (and can't skip due to no size): " + id);
                }
            }
        }

        public default void writeCommand(DataOutputStream out, boolean includeSize) throws IOException {
            byte id = switch (this) {
                case SetAnchorPos x -> SetAnchorPos.getId();
                case SetRelativePos x -> SetRelativePos.getId();
                case SetText x -> SetText.getId();
                case SetRemovalTimeout x -> SetRemovalTimeout.getId();
                case SetBillboardMode x -> SetBillboardMode.getId();
                case SetDisplayWidth x -> SetDisplayWidth.getId();
                case SetDisplayHeight x -> SetDisplayHeight.getId();
                case SetDisplayWidthAndHeight x -> SetDisplayWidthAndHeight.getId();
                case SetYaw x -> SetYaw.getId();
                case SetPitch x -> SetPitch.getId();
                case SetYawAndPitch x -> SetYawAndPitch.getId();
                case SetViewDistance x -> SetViewDistance.getId();
                case SetBackground x -> SetBackground.getId();
                case SetTextOpacity x -> SetTextOpacity.getId();
                case SetDisplayFlags x -> SetDisplayFlags.getId();
                case SetLineWidth x -> SetLineWidth.getId();
            };

            if(includeSize) {
                id = (byte) (((int) id) | 128);

                ByteArrayOutputStream bout = new ByteArrayOutputStream();
                this.writeContent(new DataOutputStream(bout));
                byte[] content = bout.toByteArray();

                if(content.length > 255)
                    throw new IllegalArgumentException("Can't write command that includes size, because the content is bigger than 255 bytes!");

                out.writeByte(id);
                out.writeByte(content.length);
                out.write(content);
            }else {
                out.writeByte(id);
                this.writeContent(out);
            }
        }
    }

    public static class State {
        public static final HashSet<State> STATES = new HashSet<>();

        private @NotNull Vec3d position;
        private @NotNull UUID ownerUuid;
        private @Nullable DisplayEntity.TextDisplayEntity entity = null;

        private long removalTimeoutMillis = 10*1000; // Default: 10s
        private long lastTouchedAtNanos;
        private State(@NotNull UUID ownerUuid, @NotNull Vec3d position) {
            this.ownerUuid = ownerUuid;
            this.position = position;
            touch();
        }

        public void touch() {
            this.lastTouchedAtNanos = System.nanoTime();
        }

        private void removeEntity(ClientWorld world) {
            if(entity == null) return;
            world.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED);
            entity = null;
        }

        private void spawnEntity(ClientWorld world) {
            removeEntity(world);

            // Find new suitable entity id
            @Nullable Integer entityId = null;
            int lowestEntityId = Integer.MAX_VALUE;
            for(Entity e : world.getEntities()) if(e.getId() < lowestEntityId) lowestEntityId = e.getId();
            int idTestingStart = lowestEntityId - (Integer.MAX_VALUE / 2);
            for(int idOffset = 0; idOffset < 1024; idOffset++) {
                int maybeEntityId = idTestingStart + idOffset;
                if(world.getEntityById(maybeEntityId) == null) {
                    entityId = maybeEntityId;
                    break;
                }
            }

            if(entityId == null)
                throw new RuntimeException("Failed to find new suitable entity id for ProxChat TextDisplay Entity");

            entity = new DisplayEntity.TextDisplayEntity(EntityType.TEXT_DISPLAY, world);
            entity.setId(entityId);
            entity.setBillboardMode(DisplayEntity.BillboardMode.CENTER);
            entity.refreshPositionAfterTeleport(position);
            world.addEntity(entity);
        }

        public DisplayEntity.TextDisplayEntity getEntity(ClientWorld world) {
            if(entity != null && world != entity.getWorld()) removeEntity(world);
            if(entity == null || entity.isRemoved()) spawnEntity(world);
            return entity;
        }

        public void handle(ClientWorld world, UUID senderUuid, Command command) {
            touch();
            this.ownerUuid = senderUuid;

            switch (command) {
                case Command.SetRemovalTimeout setRemovalTimeout -> this.removalTimeoutMillis = setRemovalTimeout.removalTimeoutMillis;
                case Command.SetText setText -> {
                    if(setText.text.isEmpty()) {
                        removeEntity(world);
                    } else {
                        getEntity(world).setText(Text.literal(setText.text));
                    }
                }
                case Command.SetBillboardMode setBillboardMode -> getEntity(world).setBillboardMode(setBillboardMode.mode);
                case Command.SetDisplayWidth setDisplayWidth -> getEntity(world).setDisplayWidth(setDisplayWidth.displayWidth);
                case Command.SetDisplayHeight setDisplayHeight -> getEntity(world).setDisplayHeight(setDisplayHeight.displayHeight);
                case Command.SetDisplayWidthAndHeight setDisplayWidthAndHeight -> {
                    getEntity(world).setDisplayWidth(setDisplayWidthAndHeight.displayWidth);
                    getEntity(world).setDisplayHeight(setDisplayWidthAndHeight.displayHeight);
                }
                case Command.SetYaw setYaw -> getEntity(world).setYaw(setYaw.yaw);
                case Command.SetPitch setPitch -> getEntity(world).setPitch(setPitch.pitch);
                case Command.SetYawAndPitch setYawAndPitch -> {
                    getEntity(world).setYaw(setYawAndPitch.yaw);
                    getEntity(world).setPitch(setYawAndPitch.pitch);
                }
                case Command.SetViewDistance setViewDistance -> getEntity(world).setViewRange((float) (setViewDistance.viewDistance / 64.0f / DisplayEntity.getRenderDistanceMultiplier()));
                case Command.SetBackground setBackground -> getEntity(world).setBackground(setBackground.backgroundArgb);
                case Command.SetTextOpacity setTextOpacity -> getEntity(world).setTextOpacity(setTextOpacity.textOpacity);
                case Command.SetDisplayFlags setDisplayFlags -> getEntity(world).setDisplayFlags(setDisplayFlags.toFlags());
                case Command.SetLineWidth setLineWidth -> getEntity(world).setLineWidth(setLineWidth.lineWidth);

                case Command.SetAnchorPos x -> throw new IllegalArgumentException("Command.SetAnchorPos should not be passed to a specific State!");
                case Command.SetRelativePos x -> throw new IllegalArgumentException("Command.SetRelativePos should not be passed to a specific State!");
            }
        }

        public boolean shouldRemoveThis() {
            return System.nanoTime() - lastTouchedAtNanos > removalTimeoutMillis * 1_000_000;
        }

        public static State create(@NotNull UUID senderUuid, @NotNull Vec3d position) {
            State state = new State(senderUuid, position);
            STATES.add(state);
            return state;
        }

        public void removeThis(ClientWorld world) {
            removeEntity(world);
            STATES.remove(this);
        }

        public static @Nullable TextDisplay.State find(Vec3d position) {
            for (State state : STATES) {
                if(state.position.squaredDistanceTo(position) <= 0.1 * 0.1)
                    return state;
            }
            return null;
        }

        public static void tickAll(@Nullable ClientWorld world) {
            HashSet<State> removeStates = new HashSet<>();
            for(State state : STATES) {
                if(state.shouldRemoveThis() || world == null)
                    removeStates.add(state);
            }

            for(State removeState : removeStates)
                removeState.removeThis(world);
        }
    }
}
