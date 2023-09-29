package io.wispforest.owo.kodeck;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.TriFunction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Kodeck<T> {

    //@formatter:off
//    Kodeck<Byte> BYTE = new Kodeck<>() {
//        @Override public <E> Byte decode(Format<E> ops, E object) { return ops.getByte(object); }
//        @Override public <E> E encode(Format<E> ops, Byte object) { return ops.createByte(object); }
//    };
//    Kodeck<Short> SHORT = new Kodeck<>() {
//        @Override public <E> Short decode(Format<E> ops, E object) { return ops.getShort(object); }
//        @Override public <E> E encode(Format<E> ops, Short object) { return ops.createShort(object); }
//    };
//    Kodeck<Integer> INT = new Kodeck<>() {
//        @Override public <E> Integer decode(Format<E> ops, E object) { return ops.getInt(object); }
//        @Override public <E> E encode(Format<E> ops, Integer object) { return ops.createInt(object); }
//    };
//    Kodeck<Long> LONG = new Kodeck<>() {
//        @Override public <E> Long decode(Format<E> ops, E object) { return ops.getLong(object); }
//        @Override public <E> E encode(Format<E> ops, Long object) { return ops.createLong(object); }
//    };
//    Kodeck<Float> FLOAT = new Kodeck<>() {
//        @Override public <E> Float decode(Format<E> ops, E object) { return ops.getFloat(object); }
//        @Override public <E> E encode(Format<E> ops, Float object) { return ops.createFloat(object); }
//    };
//    Kodeck<Double> DOUBLE = new Kodeck<>() {
//        @Override public <E> Double decode(Format<E> ops, E object) { return ops.getDouble(object); }
//        @Override public <E> E encode(Format<E> ops, Double object) { return ops.createDouble(object); }
//    };
//    Kodeck<String> STRING = new Kodeck<>() {
//        @Override public <E> String decode(Format<E> ops, E object) { return ops.getString(object); }
//        @Override public <E> E encode(Format<E> ops, String object) { return ops.createString(object); }
//    };
    Kodeck<Byte> BYTE = Kodeck.of(Format::getByte, Format::createByte);
    Kodeck<Short> SHORT = Kodeck.of(Format::getShort, Format::createShort);
    Kodeck<Integer> INT = Kodeck.of(Format::getInt, Format::createInt);
    Kodeck<Long> LONG = Kodeck.of(Format::getLong, Format::createLong);
    Kodeck<Float> FLOAT = Kodeck.of(Format::getFloat, Format::createFloat);
    Kodeck<Double> DOUBLE = Kodeck.of(Format::getDouble, Format::createDouble);
    Kodeck<String> STRING = Kodeck.of(Format::getString, Format::createString);

    Kodeck<int[]> INT_ARRAY = ListKodeck.of(INT)
            .then(
                    integers -> integers.stream().mapToInt(value -> value).toArray(),
                    ints -> Arrays.stream(ints).boxed().toList()
            );
    Kodeck<byte[]> BYTE_ARRAY = ListKodeck.of(Kodeck.BYTE)
            .then(
                    list -> {
                        byte[] bytes = new byte[list.size()];
                        for (int i = 0; i < list.size(); i++) bytes[i] = list.get(i);
                        return bytes;
                    },
                    bytes -> {
                        List<Byte> list = new ArrayList<>();
                        for (int i = 0; i < bytes.length; i++) list.add(bytes[i]);
                        return list;
                    }
            );
    Kodeck<long[]> LONG_ARRAY = ListKodeck.of(LONG)
            .then(
                    longs -> longs.stream().mapToLong(value -> value).toArray(),
                    longs -> Arrays.stream(longs).boxed().toList()
            );

    Kodeck<Identifier> IDENTIFIER = Kodeck.STRING.then(Identifier::new, Identifier::toString);
    Kodeck<Boolean> BOOLEAN = Kodeck.of(Format::getBoolean, Format::createBoolean);

    //--

    Kodeck<JsonElement> JSON_ELEMENT = new FormatKodeck<>(JsonFormat.INSTANCE, element -> {
        if (element instanceof JsonNull) {
            return (byte) 0;
        } else if (element instanceof JsonPrimitive primitive) {
            if (primitive.isString()) {
                return (byte) 8;
            } else if (primitive.isBoolean()) {
                return (byte) 1;
            }

            BigDecimal value = primitive.getAsBigDecimal();

            try {
                long l = value.longValueExact();

                if ((byte) l == l) {
                    return (byte) 2;
                } else if ((short) l == l) {
                    return (byte) 3;
                } else if ((int) l == l) {
                    return (byte) 4;
                }

                return (byte) 5;
            } catch (ArithmeticException var10) {
                double d = value.doubleValue();

                if ((float) d == d) {
                    return (byte) 6;
                }

                return (byte) 7;
            }
        } else if (element instanceof JsonArray) {
            return (byte) 9;
        } else if (element instanceof JsonObject) {
            return (byte) 10;
        }

        throw new IllegalStateException("Unknown JsonElement Object: " + element);
    });

    Kodeck<NbtElement> NBT_ELEMENT = new FormatKodeck<>(NbtFormat.SAFE,
            kodeck -> {
                var dataHandlerMap = kodeck.dataHandlerMap;

                dataHandlerMap.put((byte) 0, null);
                dataHandlerMap.put((byte) 1, Kodeck.BYTE);
                dataHandlerMap.put((byte) 2, Kodeck.SHORT);
                dataHandlerMap.put((byte) 3, Kodeck.INT);
                dataHandlerMap.put((byte) 4, Kodeck.LONG);
                dataHandlerMap.put((byte) 5, Kodeck.FLOAT);
                dataHandlerMap.put((byte) 6, Kodeck.DOUBLE);

                var listKodeck = ListKodeck.of(kodeck);
                dataHandlerMap.put((byte) 7, listKodeck);
                dataHandlerMap.put((byte) 9, listKodeck);
                dataHandlerMap.put((byte) 11, listKodeck);
                dataHandlerMap.put((byte) 12, listKodeck);

                dataHandlerMap.put((byte) 8, Kodeck.STRING);
                dataHandlerMap.put((byte) 10, MapKodeck.of(kodeck));
            },
            NbtElement::getType);


    Kodeck<NbtCompound> COMPOUND = MapKodeck.of(NBT_ELEMENT)
            .then(NbtCompound::new, NbtCompound::toMap);

    Kodeck<ItemStack> ITEM_STACK = COMPOUND
            .then(ItemStack::fromNbt, stack -> stack.writeNbt(new NbtCompound()));

    //@formatter:on

    //--

    //Kinda mega cursed but...
    static <e, T> Kodeck<T> of(BiFunction<Format<e>, e, T> decode, TriFunction<Format<e>, T, e, e> encode){
        return new Kodeck<>() {
            @Override public <E> T decode(Format<E> ops, E object) {
                return decode.apply((Format<e>) ops, (e) object);
            }

            @Override public <E> E encode(Format<E> ops, T object, E prefix) {
                return (E) encode.apply((Format<e>) ops, object, (e) prefix);
            }
        };
    }

    static <T> Kodeck<T> ofRegistry(Registry<T> registry) {
        return Kodeck.IDENTIFIER.then(registry::get, registry::getId);
    }

    //--

    <E> T decode(Format<E> ops, E object);

    default <E> E encode(Format<E> ops, T object){
        return encode(ops, object, ops.empty());
    }

    <E> E encode(Format<E> ops, T object, E prefix);

    default <R> Kodeck<R> then(Function<T, R> getter, Function<R, T> setter) {
        return new Kodeck<>() {
            @Override
            public <E> R decode(Format<E> ops, E object) {
                return getter.apply(Kodeck.this.decode(ops, object));
            }

            @Override
            public <E> E encode(Format<E> ops, R object, E prefix) {
                return Kodeck.this.encode(ops, setter.apply(object), prefix);
            }
        };
    }
}
