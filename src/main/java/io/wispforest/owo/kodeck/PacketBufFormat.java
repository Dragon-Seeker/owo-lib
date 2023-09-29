package io.wispforest.owo.kodeck;

import com.google.common.collect.Streams;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class PacketBufFormat implements Format<PacketByteBuf> {

    public static PacketBufFormat INSTANCE = new PacketBufFormat();

    @Override
    public PacketByteBuf empty() {
        return PacketByteBufs.create();
    }

    @Override
    public PacketByteBuf createBoolean(boolean value, PacketByteBuf prefix) {
        prefix.writeBoolean(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createByte(byte value, PacketByteBuf prefix) {
        prefix.writeByte(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createShort(short value, PacketByteBuf prefix) {
        prefix.writeShort(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createInt(int value, PacketByteBuf prefix) {
        prefix.writeInt(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createLong(long value, PacketByteBuf prefix) {
        prefix.writeLong(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createFloat(float value, PacketByteBuf prefix) {
        prefix.writeFloat(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createDouble(double value, PacketByteBuf prefix) {
        prefix.writeDouble(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createString(String value, PacketByteBuf prefix) {
        prefix.writeString(value);

        return prefix;
    }

    @Override
    public PacketByteBuf createStringBasedMap(int size, PacketByteBuf prefix) {
        prefix.writeVarInt(size);

        return prefix;
    }

    @Override
    public PacketByteBuf addMapEntry(String key, Supplier<PacketByteBuf> input, PacketByteBuf prefix) {
        prefix.writeString(key);

        input.get();

        return prefix;
    }

    @Override
    public PacketByteBuf createList(int size, PacketByteBuf prefix) {
        prefix.writeVarInt(size);

        return prefix;
    }

    @Override
    public PacketByteBuf addListEntry(PacketByteBuf input, PacketByteBuf prefix) {
        return prefix;
    }

    //--

    @Override
    public boolean getBoolean(PacketByteBuf input) {
        return input.readBoolean();
    }

    @Override
    public byte getByte(PacketByteBuf input) {
        return input.readByte();
    }

    @Override
    public short getShort(PacketByteBuf input) {
        return input.readShort();
    }

    @Override
    public int getInt(PacketByteBuf input) {
        return input.readInt();
    }

    @Override
    public long getLong(PacketByteBuf input) {
        return input.readLong();
    }

    @Override
    public float getFloat(PacketByteBuf input) {
        return input.readFloat();
    }

    @Override
    public double getDouble(PacketByteBuf input) {
        return input.readDouble();
    }

    @Override
    public String getString(PacketByteBuf input) {
        return input.readString();
    }

    @Override
    public Stream<Map.Entry<String, PacketByteBuf>> getStringBasedMap(PacketByteBuf input) {
        var entryTotal = input.readVarInt();

        return Streams.stream(new CursedIterator<>(entryTotal, new CursedMapEntry(input)));
    }

    @Override
    public Stream<PacketByteBuf> getList(PacketByteBuf input) {
        var entryTotal = input.readVarInt();

        return Streams.stream(new CursedIterator<>(entryTotal, input));
    }

    public record CursedMapEntry(PacketByteBuf input) implements Map.Entry<String, PacketByteBuf>{
        @Override
        public String getKey() {
            return input.readString();
        }

        @Override
        public PacketByteBuf getValue() {
            return input;
        }

        @Override public PacketByteBuf setValue(PacketByteBuf value) {return null;}
    }

    public static final class CursedIterator<E> implements Iterator<E> {
        private final int size;
        private final E value;

        private int index = 0;

        public CursedIterator(int size, E value) {
            this.size = size;
            this.value = value;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        @Override
        public E next() {
            index++;

            return value;
        }
    }
}
