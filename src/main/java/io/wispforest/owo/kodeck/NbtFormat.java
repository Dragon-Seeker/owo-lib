package io.wispforest.owo.kodeck;

import net.minecraft.nbt.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class NbtFormat implements Format<NbtElement> {

    public static NbtFormat SAFE = new NbtFormat(false);
    public static NbtFormat UNSAFE = new NbtFormat(true);

    private final boolean unsafe;

    private NbtFormat(boolean unsafe){
        this.unsafe = unsafe;
    }

//    public <U> U convertTo(Format<U> outFormat, NbtElement nbtElement) {
//        if (outFormat instanceof NbtFormat) {
//            return (U)nbtElement;
//        } else {
//            return (U)(switch(nbtElement.getType()) {
//                case 0 -> outFormat.empty();
//                case 1 -> outFormat.createByte(((AbstractNbtNumber)nbtElement).byteValue());
//                case 2 -> outFormat.createShort(((AbstractNbtNumber)nbtElement).shortValue());
//                case 3 -> outFormat.createInt(((AbstractNbtNumber)nbtElement).intValue());
//                case 4 -> outFormat.createLong(((AbstractNbtNumber)nbtElement).longValue());
//                case 5 -> outFormat.createFloat(((AbstractNbtNumber)nbtElement).floatValue());
//                case 6 -> outFormat.createDouble(((AbstractNbtNumber)nbtElement).doubleValue());
//                default -> throw new IllegalStateException("Unknown tag type: " + nbtElement);
//                case 8 -> outFormat.createString(nbtElement.asString());
//                case 10 -> {
//                    Map<String, U> map = (Map)this.getStringBasedMap(nbtElement)
//                            .entrySet()
//                            .stream()
//                            .collect(Collectors.toMap(Map.Entry::getKey, entry -> this.convertTo(outFormat, (NbtElement)entry.getValue())));
//                    yield outFormat.createStringBasedMap(map);
//                }
//                case 15 -> outFormat.createList(this.getList(nbtElement).stream().map(element -> this.convertTo(outFormat, element)).toList());
//            });
//        }
//    }

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
        return new NbtCompound();
    }

    @Override
    public NbtElement addMapEntry(String key, Supplier<NbtElement> input, NbtElement prefix) {
        return ((NbtCompound) prefix).put(key, input.get());
    }

    @Override
    public NbtElement createList(int size, NbtElement prefix) {
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
}
