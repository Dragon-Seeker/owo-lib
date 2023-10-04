package io.wispforest.owo.serialization;

import com.google.gson.*;
import io.wispforest.owo.serialization.impl.JsonFormat;
import io.wispforest.owo.serialization.impl.NbtFormat;
import io.wispforest.owo.serialization.impl.kodecks.FormatKodeck;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
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
    Kodeck<Boolean> BOOLEAN = Kodeck.of(Format::getBoolean, Format::createBoolean);
    Kodeck<Byte> BYTE = Kodeck.of(Format::getByte, Format::createByte);
    Kodeck<Short> SHORT = Kodeck.of(Format::getShort, Format::createShort);
    Kodeck<Integer> INT = Kodeck.of(Format::getInt, Format::createInt);
    Kodeck<Long> LONG = Kodeck.of(Format::getLong, Format::createLong);
    Kodeck<Float> FLOAT = Kodeck.of(Format::getFloat, Format::createFloat);
    Kodeck<Double> DOUBLE = Kodeck.of(Format::getDouble, Format::createDouble);
    Kodeck<String> STRING = Kodeck.of(Format::getString, Format::createString);

    Kodeck<byte[]> BYTE_ARRAY = ListKodeck.of(Kodeck.BYTE)
            .then(list -> {
                byte[] bytes = new byte[list.size()];
                for (int i = 0; i < list.size(); i++) bytes[i] = list.get(i);
                return bytes;
            }, bytes -> {
                List<Byte> list = new ArrayList<>();
                for (byte Byte : bytes) list.add(Byte);
                return list;
            });

    Kodeck<int[]> INT_ARRAY = ListKodeck.of(INT)
            .then((list) -> list.stream().mapToInt(v -> v).toArray(), (ints) -> Arrays.stream(ints).boxed().toList());

    Kodeck<long[]> LONG_ARRAY = ListKodeck.of(LONG)
            .then((list) -> list.stream().mapToLong(v -> v).toArray(), (longs) -> Arrays.stream(longs).boxed().toList());

    Kodeck<Identifier> IDENTIFIER = Kodeck.STRING.then(Identifier::new, Identifier::toString);

    //--

    Kodeck<JsonElement> JSON_ELEMENT = new FormatKodeck<>(JsonFormat.INSTANCE, element -> {
        if (element instanceof JsonNull) return (byte) 0;

        if (element instanceof JsonPrimitive primitive) {
            if (primitive.isString()) return (byte) 8;
            if (primitive.isBoolean()) return (byte) 1;

            BigDecimal value = primitive.getAsBigDecimal();

            try {
                long l = value.longValueExact();

                if ((byte) l == l) return (byte) 2;
                if ((short) l == l) return (byte) 3;
                if ((int) l == l) return (byte) 4;

                return (byte) 5;
            } catch (ArithmeticException var10) {
                double d = value.doubleValue();

                if ((float) d == d) return (byte) 6;

                return (byte) 7;
            }
        }

        if (element instanceof JsonArray) return (byte) 9;
        if (element instanceof JsonObject) return (byte) 10;

        throw new IllegalStateException("Unknown JsonElement Object: " + element);
    });

    Kodeck<NbtElement> NBT_ELEMENT = new FormatKodeck<>(NbtFormat.SAFE,
            kodeck -> {
                kodeck.addToMap(NbtElement.END_TYPE, null);
                kodeck.addToMap(NbtElement.BYTE_TYPE, Kodeck.BYTE);
                kodeck.addToMap(NbtElement.SHORT_TYPE, Kodeck.SHORT);
                kodeck.addToMap(NbtElement.INT_TYPE, Kodeck.INT);
                kodeck.addToMap(NbtElement.LONG_TYPE, Kodeck.LONG);
                kodeck.addToMap(NbtElement.FLOAT_TYPE, Kodeck.FLOAT);
                kodeck.addToMap(NbtElement.DOUBLE_TYPE, Kodeck.DOUBLE);

                kodeck.addToMap(NbtElement.BYTE_ARRAY_TYPE,
                        Kodeck.BYTE_ARRAY.then(NbtByteArray::new, NbtByteArray::getByteArray));

                kodeck.addToMap(NbtElement.LIST_TYPE,  ListKodeck.of(kodeck));

                kodeck.addToMap(NbtElement.INT_ARRAY_TYPE,
                        Kodeck.INT_ARRAY.then(NbtIntArray::new, NbtIntArray::getIntArray));

                kodeck.addToMap(
                        NbtElement.LONG_ARRAY_TYPE,
                        Kodeck.LONG_ARRAY
                                .then(NbtLongArray::new, NbtLongArray::getLongArray)
                );

                kodeck.addToMap(NbtElement.STRING_TYPE, Kodeck.STRING);
                kodeck.addToMap(NbtElement.COMPOUND_TYPE, MapKodeck.of(kodeck));
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
