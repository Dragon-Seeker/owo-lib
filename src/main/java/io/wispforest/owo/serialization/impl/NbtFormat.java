package io.wispforest.owo.serialization.impl;

import io.wispforest.owo.serialization.impl.kodecks.FormatKodeck;
import io.wispforest.owo.serialization.Kodeck;
import io.wispforest.owo.serialization.StructuredFormat;
import net.minecraft.nbt.*;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class NbtFormat implements StructuredFormat<NbtElement, NbtCompound> {

    public static NbtFormat SAFE = new NbtFormat(false);
    public static NbtFormat UNSAFE = new NbtFormat(true);

    private final boolean unsafe;

    private NbtFormat(boolean unsafe){
        this.unsafe = unsafe;
    }

    @Override
    public FormatKodeck<NbtElement> getFormatKodeck() {
        return (FormatKodeck<NbtElement>) Kodeck.NBT_ELEMENT;
    }

    //--

    @Override
    public NbtElement empty() {
        return NbtEnd.INSTANCE;
    }

    @Override
    public NbtElement createBoolean(boolean value, NbtElement prefix) {
        return NbtByte.of(value);
    }

    @Override
    public NbtElement createByte(byte value, NbtElement prefix) {
        return NbtByte.of(value);
    }

    @Override
    public NbtElement createShort(short value, NbtElement prefix) {
        return NbtShort.of(value);
    }

    @Override
    public NbtElement createInt(int value, NbtElement prefix) {
        return NbtInt.of(value);
    }

    @Override
    public NbtElement createLong(long value, NbtElement prefix) {
        return NbtLong.of(value);
    }

    @Override
    public NbtElement createFloat(float value, NbtElement prefix) {
        return NbtFloat.of(value);
    }

    @Override
    public NbtElement createDouble(double value, NbtElement prefix) {
        return NbtDouble.of(value);
    }

    @Override
    public NbtElement createString(String value, NbtElement prefix) {
        return NbtString.of(value);
    }

    @Override
    public NbtElement createStringBasedMap(int size, NbtElement prefix) {
        if(prefix instanceof NbtCompound prefixCompound) return prefixCompound;

        return new NbtCompound();
    }

    @Override
    public NbtElement addMapEntry(String key, Supplier<NbtElement> input, NbtElement prefix) {
        return ((NbtCompound) prefix).put(key, input.get());
    }

    @Override
    public NbtElement createList(int size, NbtElement prefix) {
        if(prefix instanceof NbtList prefixList) return prefixList;

        return new NbtList();
    }

    @Override
    public NbtElement addListEntry(NbtElement input, NbtElement prefix) {
        ((NbtList) prefix).add(input);

        return prefix;
    }

    //---


    @Override
    public boolean getBoolean(NbtElement input) {
        if(input instanceof NbtByte nbtNumber) return nbtNumber.byteValue() != 0;

        if(unsafe) return false; // Default value: 0 != 0;

        throw new RuntimeException("[NbtFormat] input was not NbtByte for a Boolean get call");
    }

    @Override
    public byte getByte(NbtElement input) {
        if(input instanceof NbtByte nbtNumber) return nbtNumber.byteValue();

        if(unsafe) return 0;

        throw new RuntimeException("[NbtFormat] input was not NbtByte for a Byte get call");
    }

    @Override
    public short getShort(NbtElement input) {
        if(input instanceof NbtShort nbtNumber) return nbtNumber.shortValue();

        if(unsafe) return 0;

        throw new RuntimeException("[NbtFormat] input was not NbtShort");
    }

    @Override
    public int getInt(NbtElement input) {
        if(input instanceof NbtInt nbtNumber) return nbtNumber.intValue();

        if(unsafe) return 0;

        throw new RuntimeException("[NbtFormat] input was not NbtInt");
    }

    @Override
    public long getLong(NbtElement input) {
        if(input instanceof NbtLong nbtNumber) return nbtNumber.longValue();

        if(unsafe) return 0L;

        throw new RuntimeException("[NbtFormat] input was not NbtLong");
    }

    @Override
    public float getFloat(NbtElement input) {
        if(input instanceof NbtFloat nbtNumber) return nbtNumber.floatValue();

        if(unsafe) return 0F;

        throw new RuntimeException("[NbtFormat] input was not NbtFloat");
    }

    @Override
    public double getDouble(NbtElement input) {
        if(input instanceof NbtDouble nbtNumber) return nbtNumber.doubleValue();

        if(unsafe) return 0D;

        throw new RuntimeException("[NbtFormat] input was not NbtDouble");
    }

    @Override
    public String getString(NbtElement input) {
        if(input instanceof NbtString nbtString) return nbtString.asString();

        if(unsafe) return "";

        throw new RuntimeException("[NbtFormat] input was not NbtDouble");
    }

    @Override
    public Stream<Map.Entry<String, NbtElement>> getStringBasedMap(NbtElement input) {
        if(input instanceof NbtCompound compound) return compound.toMap().entrySet().stream();

        if(unsafe) return Stream.of();

        throw new RuntimeException("[NbtFormat] input's entry was not NbtCompound");
    }

    @Override
    public Stream<NbtElement> getList(NbtElement input) {
        if(input instanceof AbstractNbtList nbtList) return nbtList.stream();

        if(unsafe) return Stream.of();

        throw new RuntimeException("[NbtFormat] input was not NbtList");
    }

    //--


    @Override
    public NbtElement get(NbtCompound map, String key) {
        return map.get(key);
    }

    @Override
    public NbtElement put(NbtCompound map, String key, NbtElement value) {
        return map.put(key, value);
    }

    @Override
    public NbtElement delete(NbtCompound map, String key) {
        var value = map.get(key);

        map.remove(key);

        return value;
    }

    @Override
    public boolean contains(NbtCompound map, String key) {
        return false;
    }
}
