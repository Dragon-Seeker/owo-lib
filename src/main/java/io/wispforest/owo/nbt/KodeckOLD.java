package io.wispforest.owo.nbt;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.*;

/**
 * A container type holding serialization functions,
 * used for handling various {@link NbtElement}'s
 */
public class KodeckOLD<T, E extends NbtElement> {

    public static final KodeckOLD<Byte, NbtByte> BYTE = new KodeckOLD<>(NbtElement.BYTE_TYPE, NbtByte::byteValue, NbtByte::of, () -> NbtByte.of((byte) 0));
    public static final KodeckOLD<Short, NbtShort> SHORT = new KodeckOLD<>(NbtElement.SHORT_TYPE, NbtShort::shortValue, NbtShort::of, () -> NbtShort.of((short) 0));
    public static final KodeckOLD<Integer, NbtInt> INT = new KodeckOLD<>(NbtElement.INT_TYPE, NbtInt::intValue, NbtInt::of, () -> NbtInt.of(0));
    public static final KodeckOLD<Long, NbtLong> LONG = new KodeckOLD<>(NbtElement.LONG_TYPE, NbtLong::longValue, NbtLong::of, () -> NbtLong.of(0L));
    public static final KodeckOLD<Float, NbtFloat> FLOAT = new KodeckOLD<>(NbtElement.FLOAT_TYPE, NbtFloat::floatValue, NbtFloat::of, () -> NbtFloat.of(0F));
    public static final KodeckOLD<Double, NbtDouble> DOUBLE = new KodeckOLD<>(NbtElement.DOUBLE_TYPE, NbtDouble::doubleValue, NbtDouble::of, () -> NbtDouble.of(0D));
    public static final KodeckOLD<byte[], NbtByteArray> BYTE_ARRAY = new KodeckOLD<>(NbtElement.BYTE_ARRAY_TYPE, NbtByteArray::getByteArray, NbtByteArray::new, () -> new NbtByteArray(new byte[0]));
    public static final KodeckOLD<String, NbtString> STRING = new KodeckOLD<>(NbtElement.STRING_TYPE, NbtString::asString, NbtString::of, () -> NbtString.of(""));
    public static final KodeckOLD<NbtCompound, NbtCompound> COMPOUND = new KodeckOLD<>(NbtElement.COMPOUND_TYPE, compound -> compound, compound -> compound, NbtCompound::new);
    public static final KodeckOLD<int[], NbtIntArray> INT_ARRAY = new KodeckOLD<>(NbtElement.INT_ARRAY_TYPE, NbtIntArray::getIntArray, NbtIntArray::new, () -> new NbtIntArray(new int[0]));
    public static final KodeckOLD<long[], NbtLongArray> LONG_ARRAY = new KodeckOLD<>(NbtElement.LONG_ARRAY_TYPE, NbtLongArray::getLongArray, NbtLongArray::new, () -> new NbtLongArray(new long[0]));
    public static final KodeckOLD<ItemStack, NbtCompound> ITEM_STACK = new KodeckOLD<>(NbtElement.COMPOUND_TYPE, KodeckOLD::readItemStack, KodeckOLD::writeItemStack, NbtCompound::new);
    public static final KodeckOLD<Identifier, NbtString> IDENTIFIER = new KodeckOLD<>(NbtElement.STRING_TYPE, KodeckOLD::readIdentifier, KodeckOLD::writeIdentifier, () -> NbtString.of(""));
    public static final KodeckOLD<Boolean, NbtByte> BOOLEAN = new KodeckOLD<>(NbtElement.BYTE_TYPE, nbtByte -> nbtByte.byteValue() != 0, NbtByte::of, () -> NbtByte.of(false));
    public static final KodeckOLD<NbtList, NbtList> LIST = new KodeckOLD<>(NbtElement.LIST_TYPE, nbtList -> nbtList, nbtList -> nbtList, NbtList::new);

    protected final byte nbtEquivalent;

    protected final Function<E, T> fromElement;
    protected final BiFunction<T, Supplier<E>, E> toElement;

    protected final Supplier<E> elementFactory;

    private KodeckOLD(byte nbtEquivalent, Function<E, T> fromElement, BiFunction<T, Supplier<E>, E> toElement, Supplier<E> elementFactory) {
        this.nbtEquivalent = nbtEquivalent;

        this.fromElement = fromElement;
        this.toElement = toElement; //Altered for NbtCompound use to a BiFunction to allow for factory

        this.elementFactory = elementFactory;
    }

    private KodeckOLD(byte nbtEquivalent, Function<E, T> fromElement, Function<T, E> toElement, Supplier<E> elementFactory) {
        this(nbtEquivalent, fromElement, (t, factory) -> toElement.apply(t), elementFactory);
    }

    public static KeyedKodeck<List<Text>> TEST_KODECK = KodeckOLD.listType(KodeckOLD.STRING).fieldOf("TEST")
            .then(strings -> strings.stream().map(s -> Text.of(s)).toList(), texts -> texts.stream().map(text -> text.toString()).toList());

    static {
        List<Text> texts = KeyedKodeck.get(TEST_KODECK, new NbtCompound());

        KeyedKodeck.put(TEST_KODECK, new NbtCompound(), texts);
    }

//    public static class Odeck<T> {
//        public BiFunction<Format<?>, Object, T> decode;
//        public BiFunction<Format<?>, T, Object> encode;
//
//        public <E> Odeck(BiFunction<Format<E>, E, T> decode, BiFunction<Format<E>, T, E> encode) {
//            this.encode = (format, t) -> encode.apply((Format<E>) format, t);
//            this.decode = (format, o) -> decode.apply((Format<E>) format, (E) o);
//        }
//
//        public static <E, T> Odeck<T> of(BiFunction<Format<E>, E, T> decode, BiFunction<Format<E>, T, E> encode){
//            return new Odeck<>(decode, encode);
//        }
//
//        public <E> T decode(Format<E> ops, E object){
//            return decode.apply(ops, object);
//        }
//
//        public <E> E encode(Format<E> ops, T object){
//            return (E) encode.apply(ops, object);
//        }
//    }

//    public <E> T decode(Format<E> ops, E object){
//
//    }
//
//    public <E> E encode(Format<E> ops, T object){
//
//    }

    //---

    /**
     * Creates a new Kodeck instance used for storing data of type
     * {@code T} into NBT compounds with the given string as key
     *
     * @param key  The string key to use as index into the NBT compound
     */
    public KeyedKodeck<T> fieldOf(String key){
        return new KeyedKodeck<>(
                key,
                (compound) -> {
                    return this.fromElement.apply((compound.contains(key, this.nbtEquivalent)) ? (E) compound.get(key) : this.elementFactory.get());
                },
                (t, factory) -> {
                    var compound = factory.get();

                    compound.put(key, this.toElement.apply(t, this.elementFactory));

                    return compound;
                }
        );
    }

    public static class KeyedKodeck<T> extends KodeckOLD<T, NbtCompound> {

        protected final String key;

        protected KeyedKodeck(String key, Function<NbtCompound, T> fromElement, BiFunction<T, Supplier<NbtCompound>, NbtCompound> toElement) {
            super(NbtElement.COMPOUND_TYPE, fromElement, toElement, NbtCompound::new);

            this.key = key;
        }

        public <R> KeyedKodeck<R> then(Function<T, R> getter, Function<R, T> setter) {
            return new KeyedKodeck<>(this.key, this.fromElement.andThen(getter), (r, factory) -> this.toElement.apply(setter.apply(r), factory));
        }

        //---

        /**
         * @deprecated Use {@link NbtCarrier#get(KodeckOLD)} instead
         */
        @Deprecated
        public static <T> T get(KeyedKodeck<T> type, NbtCompound compound){
            return type.fromElement.apply(compound);
        }

        /**
         * @deprecated Use {@link NbtCarrier#put(KodeckOLD, T)} instead
         */
        @Deprecated
        public static <T> void put(KeyedKodeck<T> type, NbtCompound compound, T value){
            type.toElement.apply(value, () -> compound);
        }

        /**
         * @deprecated Use {@link NbtCarrier#delete(KodeckOLD)} instead
         */
        @Deprecated
        public static <T> void delete(KeyedKodeck<T> type, NbtCompound compound){
            compound.remove(type.key);
        }

        /**
         * @deprecated Use {@link NbtCarrier#has(KodeckOLD)} instead
         */
        @Deprecated
        public static <T> boolean isIn(KeyedKodeck<T> type, @NotNull NbtCompound nbt) {
            return nbt.contains(type.key, type.nbtEquivalent);
        }
    }

    /**
     * Creates a new type that applies the given functions on top of
     * this type. This allows easily composing types by abstracting away
     * the underlying NBT compound
     *
     * @param getter The getter function to convert from this type's value type to the new one
     * @param setter The setter function to convert from the new value type to this type's one
     * @param <R>    The value type of the created type
     * @return The new key
     */
    public <R> KodeckOLD<R, E> then(Function<T, R> getter, Function<R, T> setter) {
        return new KodeckOLD<>(this.nbtEquivalent, this.fromElement.andThen(getter), r -> this.toElement.apply(setter.apply(r), this.elementFactory), this.elementFactory);
    }

    /**
     * Creates a new {@link KodeckOLD} that supports reading and writing data of type {@code T}
     * into {@link NbtCompound} instances. Use this if you want to store data that is
     * not supported by the default provided types
     *
     * @param nbtType The type of NBT element that is used to represent the data,
     *                see {@link NbtElement} for the relevant constants
     * @param fromElement  The function used to convert from the type {@code T} to the required {@code NbtElement}
     * @param toElement    The function used to convert from the {@code NbtElement} to the specified type {@code T}
     * @param defaultValue The supplier to get a default instance if none are within {@link NbtCompound}
     * @param <T>     The type of data the created key can serialize
     * @return The created Type instance
     */
    public static <T, E extends NbtElement> KodeckOLD<T, E> of(byte nbtType, Function<E, T> fromElement, Function<T, E> toElement, Supplier<E> defaultValue) {
        return new KodeckOLD<>(nbtType, fromElement, toElement, defaultValue);
    }

    /**
     * Creates a new type that serializes a List of elements of the given {@link KodeckOLD}
     *
     * @param elementType The {@link KodeckOLD} base used to serialize between {@link NbtList} elements
     * @param <T>         The type of data the passed key can serialize
     * @return The List based Type instance
     */
    public static <T, E extends NbtElement> KodeckOLD<List<T>, NbtList> listType(KodeckOLD<T, E> elementType){
        return collectionType(elementType, ArrayList::new);
    }

    /**
     *
     * @param elementType       The {@link KodeckOLD} base key used to serialize between {@link NbtList}
     * @param collectionBuilder The builder used to create new instances of a collection
     * @param <T>               The type of data the passed key can serialize
     * @param <C>               The type of collection the created Type can serialize
     * @return The Collection based type instance
     */
    public static <T, C extends Collection<T>, E extends NbtElement> KodeckOLD<C, NbtList> collectionType(KodeckOLD<T, E> elementType, IntFunction<C> collectionBuilder){
        return KodeckOLD.LIST.then(
                nbtList -> {
                    if(nbtList.getType() != elementType.nbtEquivalent) return collectionBuilder.apply(0);

                    var collection = collectionBuilder.apply(nbtList.size());
                    for (NbtElement element : nbtList) {
                        collection.add((elementType.fromElement).apply((E) element));
                    }

                    return collection;
                },
                values -> {
                    var nbtList = new NbtList();
                    for(T value : values){
                        nbtList.add(elementType.toElement.apply(value, elementType.elementFactory));
                    }

                    return nbtList;
                }
        );
    }

    /**
     * Creates a new type that serializes registry entries of the given
     * registry using their ID in string form
     *
     * @param registry The registry of which to serialize entries
     * @param <T>      The type of registry entry to serialize
     * @return The created type
     */
    public static <T> KodeckOLD<T, NbtString> ofRegistry(Registry<T> registry) {
        return KodeckOLD.IDENTIFIER.then(registry::get, registry::getId);
    }

    private static NbtCompound writeItemStack(ItemStack stack) {
        return stack.writeNbt(new NbtCompound());
    }

    private static ItemStack readItemStack(NbtCompound nbt) {
        return nbt.isEmpty() ? ItemStack.fromNbt(nbt) : ItemStack.EMPTY;
    }

    private static NbtString writeIdentifier(Identifier identifier) {
        return NbtString.of(identifier.toString());
    }

    private static Identifier readIdentifier(NbtString nbtString) {
        return new Identifier(nbtString.asString());
    }

}