package io.wispforest.owo.serialization;

import com.google.gson.*;
import io.wispforest.owo.serialization.impl.JsonFormat;
import io.wispforest.owo.serialization.impl.NbtFormat;
import io.wispforest.owo.serialization.impl.kodecks.FormatKodeck;
import io.wispforest.owo.serialization.impl.kodecks.KeyedKodeck;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Kodeck<T> {

    //@formatter:off
    Kodeck<Void> EMPTY = Kodeck.of((format, o) -> null, (format, unused, o) -> format.empty());

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

                kodeck.addToMap(NbtElement.BYTE_ARRAY_TYPE, Kodeck.BYTE_ARRAY.then(NbtByteArray::new, NbtByteArray::getByteArray));

                kodeck.addToMap(NbtElement.LIST_TYPE,  ListKodeck.of(kodeck))
                        .setAsDataReducible(NbtElement.LIST_TYPE, e -> ((Collection)e));

                kodeck.addToMap(NbtElement.INT_ARRAY_TYPE, Kodeck.INT_ARRAY.then(NbtIntArray::new, NbtIntArray::getIntArray));

                kodeck.addToMap(NbtElement.LONG_ARRAY_TYPE, Kodeck.LONG_ARRAY.then(NbtLongArray::new, NbtLongArray::getLongArray));

                kodeck.addToMap(NbtElement.STRING_TYPE, Kodeck.STRING);
                kodeck.addToMap(NbtElement.COMPOUND_TYPE, MapKodeck.of(kodeck));
            },
            NbtElement::getType);


    Kodeck<NbtCompound> COMPOUND = MapKodeck.of(NBT_ELEMENT)
            .then(NbtCompound::new, NbtCompound::toMap);

    Kodeck<ItemStack> ITEM_STACK = COMPOUND
            .then(ItemStack::fromNbt, stack -> stack.writeNbt(new NbtCompound()));

    Kodeck<UUID> UUID = Kodeck.of((format, o) -> {
        List<Long> longs = format.getList(0).map(format::getLong).toList();

        long mostSig = longs.get(0);
        long leastSig = longs.get(1);

        return new UUID(mostSig, leastSig);
    }, (format, uuid, o) -> {
        var list = format.createList(2, o);

        format.addListEntry(format.createLong(uuid.getMostSignificantBits(), o), 0);
        format.addListEntry(format.createLong(uuid.getMostSignificantBits(), o), 0);

        return list;
    });

    Kodeck<Date> DATE = Kodeck.LONG.then(Date::new, Date::getTime);

    Kodeck<PacketByteBuf> PACKET_BYTE_BUF = Kodeck.BYTE_ARRAY
            .then(
                    bytes -> {
                        var byteBuf = PacketByteBufs.create();

                        byteBuf.writeBytes(bytes);

                        return byteBuf;
                    },
                    byteBuf -> {
                        var bytes = new byte[byteBuf.readerIndex()];

                        byteBuf.readBytes(bytes);

                        return bytes;
                    }
            );

    Kodeck<BlockPos> BLOCK_POS = Kodeck.LONG.then(BlockPos::fromLong, BlockPos::asLong);
    Kodeck<ChunkPos> CHUNK_POS = Kodeck.LONG.then(ChunkPos::new, ChunkPos::toLong);

    Kodeck<BitSet> BITSET = Kodeck.LONG_ARRAY.then(BitSet::valueOf, BitSet::toLongArray);

    Kodeck<Text> TEXT = Kodeck.STRING.then(Text.Serializer::fromJson, Text.Serializer::toJson);

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

    default ListKodeck<T> list(){
        return ListKodeck.of(this);
    }

    default MapKodeck<String, T> map(){
        return MapKodeck.of(this);
    }

    default KeyedKodeck<T> keyedOf(String key){
        return new KeyedKodeck<>(key, this);
    }

    static <K, V> Kodeck<Map<K, V>> mapOf(Kodeck<K> keyKodeck, Kodeck<V> valueKodeck){
        return mapOf(keyKodeck, valueKodeck, HashMap::new);
    }

    static <L, R> Kodeck<Pair<L, R>> pairOf(Kodeck<L> leftKodeck, Kodeck<R> rightKodeck){
        return new Kodeck<>() {
            @Override
            public <E> Pair<L, R> decode(Format<E> ops, E object) {
                var pair = new Pair<L, R>(null, null);

                ops.getStringBasedMap(object).forEach(entry -> {
                    if (Objects.equals(entry.getKey(), "l")) {
                        pair.setLeft(leftKodeck.decode(ops, entry.getValue()));
                    } else if (Objects.equals(entry.getKey(), "r")) {
                        pair.setRight(rightKodeck.decode(ops, entry.getValue()));
                    }
                });

                return pair;
            }

            @Override
            public <E> E encode(Format<E> ops, Pair<L, R> object, E prefix) {
                E map = ops.createStringBasedMap(2, prefix);

                ops.addMapEntry("l", () -> leftKodeck.encode(ops, object.getLeft(), prefix), map);
                ops.addMapEntry("r", () -> rightKodeck.encode(ops, object.getRight(), prefix), map);

                return map;
            }
        };
    };

    default Kodeck<Optional<T>> optionalOf(){
        return CascadeKodeck.optionalOf(this);
    }

//    static <T> Kodeck<Optional<T>> optionalOf(Kodeck<T> kodeck){
//        return new Kodeck<>() {
//            @Override
//            public <E> Optional<T> decode(Format<E> ops, E object) {
//                MutableBoolean isPresent = new MutableBoolean();
//
//                MutableObject<T> value = new MutableObject<>(null);
//
//                ops.getStringBasedMap(object).forEach(entry -> {
//                    if (Objects.equals(entry.getKey(), "present")) {
//                        isPresent.setValue(Kodeck.BOOLEAN.decode(ops, entry.getValue()));
//                    } else if (Objects.equals(entry.getKey(), "value") && isPresent.getValue()) {
//                        value.setValue(kodeck.decode(ops, entry.getValue()));
//                    }
//                });
//
//                return Optional.ofNullable(value.getValue());
//            }
//
//            @Override
//            public <E> E encode(Format<E> ops, Optional<T> object, E prefix) {
//                E map = ops.createStringBasedMap(2, prefix);
//
//                var present = object.isPresent();
//
//                ops.addMapEntry("present", () -> Kodeck.BOOLEAN.encode(ops, present, prefix), map);
//                ops.addMapEntry("value", () -> present ? kodeck.encode(ops, object.get(), prefix) : ops.empty(), map);
//
//                return map;
//            }
//        };
//    }

    static <K, V> Kodeck<Map<K, V>> mapOf(Kodeck<K> keyKodeck, Kodeck<V> valueKodeck, Supplier<Map<K, V>> supplier){
        Kodeck<Map.Entry<K, V>> mapEntryKodeck = new Kodeck<>() {
            @Override
            public <E> Map.Entry<K, V> decode(Format<E> ops, E object) {
                MutableObject<K> key = new MutableObject<>();
                MutableObject<V> value = new MutableObject<>();

                ops.getStringBasedMap(object).forEach(entry -> {
                    if (Objects.equals(entry.getKey(), "k")) {
                        key.setValue(keyKodeck.decode(ops, entry.getValue()));
                    } else if (Objects.equals(entry.getKey(), "v")) {
                        value.setValue(valueKodeck.decode(ops, entry.getValue()));
                    } else {
                        throw new IllegalStateException("A had extra entries not under the 'k' or 'v' entries!");
                    }
                });

                return Map.entry(key.getValue(), value.getValue());
            }

            @Override
            public <E> E encode(Format<E> ops, Map.Entry<K, V> object, E prefix) {
                E map = ops.createStringBasedMap(2, prefix);

                ops.addMapEntry("k", () -> keyKodeck.encode(ops, object.getKey(), prefix), map);
                ops.addMapEntry("V", () -> valueKodeck.encode(ops, object.getValue(), prefix), map);

                return map;
            }
        };

        return ListKodeck.of(mapEntryKodeck).then(entries -> {
            Map<K, V> map = supplier.get();

            for (Map.Entry<K, V> entry : entries) map.put(entry.getKey(), entry.getValue());

            return map;
        }, kvMap -> {
            return List.copyOf(kvMap.entrySet());
        });
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
