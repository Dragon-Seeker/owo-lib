package io.wispforest.owo.kodeck;

import com.google.gson.*;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.function.TriFunction;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

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
    Kodeck<JsonElement> JSON_ELEMENT = new Kodeck<JsonElement>() {
        /*
         * byte NONE_TYPE   = 0;
         * byte BOOL_TYPE   = 1;
         * byte BYTE_TYPE   = 2;
         * byte SHORT_TYPE  = 3;
         * byte INT_TYPE    = 4;
         * byte LONG_TYPE   = 5;
         * byte FLOAT_TYPE  = 6;
         * byte DOUBLE_TYPE = 7;
         * byte STRING_TYPE = 8;
         * byte LIST_TYPE   = 9;
         * byte MAP_TYPE    = 10;
         */

        @Override
        public <E> JsonElement decode(Format<E> ops, E object) {
            if(ops instanceof NbtFormat){
                return NBT_ELEMENT.encode(JsonFormat.INSTANCE, (NbtElement) object, JsonFormat.INSTANCE.empty());
            } else if(ops instanceof JsonFormat){
                return (JsonElement) object;
            } else if(ops instanceof PacketBufFormat){
                byte dataType = ops.getByte(object);

                var prefix = JsonFormat.INSTANCE.empty();

                return switch (dataType){
                    case 0 -> prefix;
                    case 1 -> JsonFormat.INSTANCE.createBoolean(ops.getBoolean(object), prefix);
                    case 2 -> JsonFormat.INSTANCE.createByte(ops.getByte(object), prefix);
                    case 3 -> JsonFormat.INSTANCE.createShort(ops.getShort(object), prefix);
                    case 4 -> JsonFormat.INSTANCE.createInt(ops.getInt(object), prefix);
                    case 5 -> JsonFormat.INSTANCE.createLong(ops.getLong(object), prefix);
                    case 6 -> JsonFormat.INSTANCE.createFloat(ops.getFloat(object), prefix);
                    case 7 -> JsonFormat.INSTANCE.createDouble(ops.getDouble(object), prefix);
                    case 8 -> JsonFormat.INSTANCE.createString(ops.getString(object), prefix);
                    case 9 -> {
                        var list = new JsonArray();

                        list.asList().addAll(ListKodeck.getList(this::decode, ops, object));

                        yield list;
                    }
                    case 10 -> {
                        var jsonObject = new JsonObject();

                        jsonObject.asMap().putAll(MapKodeck.getMap(this::decode, ops, object));

                        yield jsonObject;
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + dataType);
                };
            } else {
                throw new IllegalStateException("Invalid Format used to decode JsonElement: " + object);
            }
        }

        @Override
        public <E> E encode(Format<E> ops, JsonElement object, E prefix) {
            if (ops instanceof JsonFormat) return (E)object;

            byte dataType;
            Supplier<E> writeOp;

            if(object instanceof JsonNull){
                dataType = 0;
                writeOp = ops::empty;
            } else if (object instanceof JsonPrimitive primitive) {
                if (primitive.isString()) {
                    dataType = 8;
                    writeOp = () -> (E)ops.createString(primitive.getAsString(), prefix);
                } else if (primitive.isBoolean()) {
                    dataType = 1;
                    writeOp = () -> (E)ops.createBoolean(primitive.getAsBoolean(), prefix);
                } else {
                    BigDecimal value = primitive.getAsBigDecimal();

                    try {
                        long l = value.longValueExact();
                        if ((byte) l == l) {
                            dataType = 2;
                            writeOp = () -> (E)ops.createByte((byte) l, prefix);
                        } else if ((short) l == l) {
                            dataType = 3;
                            writeOp = () -> (E)ops.createShort((short)((int)l), prefix);
                        } else if((int) l == l) {
                            dataType = 4;
                            writeOp = () -> ops.createInt((int)l, prefix);
                        } else {
                            dataType = 5;
                            writeOp = () -> ops.createLong(l, prefix);
                        }
                    } catch (ArithmeticException var10) {
                        double d = value.doubleValue();

                        if((float)d == d){
                            dataType = 6;
                            writeOp = () -> ops.createFloat((float)d, prefix);
                        } else {
                            dataType = 7;
                            writeOp = () -> ops.createDouble(d, prefix);
                        }
                    }
                }
            } else if (object instanceof JsonArray jsonArray) {
                dataType = 9;
                writeOp = () -> ListKodeck.handleList(this::encode, ops, jsonArray.asList(), prefix);
            } else if (object instanceof JsonObject jsonObject) {
                dataType = 10;
                writeOp = () -> MapKodeck.handleMap(this::encode, ops, jsonObject.asMap(), prefix);
            } else {
                throw new IllegalStateException("Unknown JsonElement Object: " + object);
            }

            if(ops instanceof PacketBufFormat) ops.createByte(dataType, prefix);

            return writeOp.get();
        }
    };

    Kodeck<NbtElement> NBT_ELEMENT = new Kodeck<>() {
        /*
         * byte END_TYPE = 0;
         * byte BYTE_TYPE = 1;
         * byte SHORT_TYPE = 2;
         * byte INT_TYPE = 3;
         * byte LONG_TYPE = 4;
         * byte FLOAT_TYPE = 5;
         * byte DOUBLE_TYPE = 6;
         * byte BYTE_ARRAY_TYPE = 7;
         * byte STRING_TYPE = 8;
         * byte LIST_TYPE = 9;
         * byte COMPOUND_TYPE = 10;
         * byte INT_ARRAY_TYPE = 11;
         * byte LONG_ARRAY_TYPE = 12;
         */

        @Override
        public <E> NbtElement decode(Format<E> ops, E object) {
            if(ops instanceof NbtFormat){
                return (NbtElement) object;
            } else if(ops instanceof JsonFormat){
                return JSON_ELEMENT.encode(NbtFormat.SAFE, (JsonElement) object, NbtFormat.SAFE.empty());
            } else if(ops instanceof PacketBufFormat){
                byte dataType = ops.getByte(object);

                var prefix = NbtFormat.SAFE.empty();

                return switch (dataType) {
                    case 0 -> prefix;
                    case 1 -> NbtFormat.SAFE.createByte(ops.getByte(object), prefix);
                    case 2 -> NbtFormat.SAFE.createShort(ops.getShort(object), prefix);
                    case 3 -> NbtFormat.SAFE.createInt(ops.getInt(object), prefix);
                    case 4 -> NbtFormat.SAFE.createLong(ops.getLong(object), prefix);
                    case 5 -> NbtFormat.SAFE.createFloat(ops.getFloat(object), prefix);
                    case 6 -> NbtFormat.SAFE.createDouble(ops.getDouble(object), prefix);
                    case 8 -> NbtFormat.SAFE.createString(ops.getString(object), prefix);
                    case 7, 9, 11, 12 -> {
                        var list = new NbtList();

                        list.addAll(ListKodeck.getList(this::decode, ops, object));

                        yield list;
                    }
                    case 10 -> new NbtCompound(MapKodeck.getMap(this::decode, ops, object));
                    default -> throw new IllegalStateException("Unexpected value: " + dataType);
                };
            }

            throw new IllegalStateException("Invalid Format used to decode NbtElement: " + object);
        }

        @Override
        public <E> E encode(Format<E> ops, NbtElement object, E prefix) {
            if (ops instanceof NbtFormat) return (E) object;

            if(ops instanceof PacketBufFormat) ops.createByte(object.getType(), prefix);

            return (E) (switch(object.getType()) {
                case 0 -> ops.empty();
                case 1 -> ops.createByte(((AbstractNbtNumber)object).byteValue(), prefix);
                case 2 -> ops.createShort(((AbstractNbtNumber)object).shortValue(), prefix);
                case 3 -> ops.createInt(((AbstractNbtNumber)object).intValue(), prefix);
                case 4 -> ops.createLong(((AbstractNbtNumber)object).longValue(), prefix);
                case 5 -> ops.createFloat(((AbstractNbtNumber)object).floatValue(), prefix);
                case 6 -> ops.createDouble(((AbstractNbtNumber)object).doubleValue(), prefix);
                case 8 -> ops.createString(object.asString(), prefix);
                case 7, 9, 11, 12 -> {
                    yield ListKodeck.handleList(this::encode, ops, ((AbstractNbtList<NbtElement>) object), prefix);
                }
                case 10 -> MapKodeck.handleMap(this::encode, ops, ((NbtCompound) object).toMap(), prefix);
                default -> throw new IllegalStateException("Unknown tag type: " + object);
            });
        }
    };

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
