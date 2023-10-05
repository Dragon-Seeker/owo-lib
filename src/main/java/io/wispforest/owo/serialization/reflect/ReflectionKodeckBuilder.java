package io.wispforest.owo.serialization.reflect;

import io.wispforest.owo.network.serialization.SealedPolymorphic;
import io.wispforest.owo.serialization.CascadeKodeck;
import io.wispforest.owo.serialization.Format;
import io.wispforest.owo.serialization.Kodeck;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import org.apache.commons.lang3.function.TriFunction;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ReflectionKodeckBuilder {

    private static final Map<Class<?>, Supplier<?>> COLLECTION_PROVIDERS = new HashMap<>();
    private static final Map<Class<?>, Kodeck<?>> SERIALIZERS = new HashMap<>();

    /**
     * Enables (de-)serialization for the given class
     *
     * @param clazz      The object class to serialize
     * @param serializer The serializer
     * @param <T>        The type of object to register a serializer for
     */
    public static <T> void register(Class<T> clazz, Kodeck<T> serializer) {
        if (SERIALIZERS.containsKey(clazz)) throw new IllegalStateException("Class '" + clazz.getName() + "' already has a serializer");
        SERIALIZERS.put(clazz, serializer);
    }

    /**
     * Enables (de-)serialization for the given class
     *
     * @param clazz        The object class to serialize
     * @param serializer   The serialization method
     * @param deserializer The deserialization method
     * @param <T>          The type of object to register a serializer for
     */
    public static <E, T> void register(Class<T> clazz, BiFunction<Format<E>, E, T> deserializer, TriFunction<Format<E>, T, E, E> serializer) {
        register(clazz, Kodeck.of(deserializer, serializer));
    }

    @SafeVarargs
    private static <T> void register(Kodeck<T> kodeck, Class<T>... classes) {
        for (var clazz : classes) register(clazz, kodeck);
    }

    @SafeVarargs
    private static <E, T> void register(BiFunction<Format<E>, E, T> deserializer, TriFunction<Format<E>, T, E, E> serializer, Class<T>... classes) {
        final var kodeck = Kodeck.of(deserializer, serializer);
        for (var clazz : classes) {
            register(clazz, kodeck);
        }
    }

    /**
     * Gets the serializer for the given class, using additional data from
     * generics, or throws an exception if none is registered
     *
     * @param type The type to obtain a serializer for
     * @return The respective serializer instance
     */
    @SuppressWarnings("unchecked")
    public static Kodeck<?> getGeneric(Type type) {
        if (type instanceof Class<?> klass) return get(klass);

        var pType = (ParameterizedType) type;
        Class<?> raw = (Class<?>) pType.getRawType();
        var typeArgs = pType.getActualTypeArguments();

        if (Map.class.isAssignableFrom(raw)) {
            return ReflectionKodeckBuilder.createMapSerializer(conform(raw, Map.class), (Class<?>) typeArgs[0], (Class<?>) typeArgs[1]);
        }

        if (Collection.class.isAssignableFrom(raw)) {
            return ReflectionKodeckBuilder.createCollectionSerializer(conform(raw, Collection.class), (Class<?>) typeArgs[0]);
        }

        if (Optional.class.isAssignableFrom(raw)) {
            return ReflectionKodeckBuilder.createOptionalSerializer((Class<?>) typeArgs[0]);
        }

        return get(raw);
    }

    /**
     * Gets the serializer for the given class, or throws
     * an exception if none is registered
     *
     * @param clazz The class to obtain a serializer for
     * @return The respective serializer instance
     */
    public static <T> Kodeck<T> get(Class<T> clazz) {
        Kodeck<T> serializer = getOrNull(clazz);

        if (serializer == null) {
            throw new IllegalStateException("No serializer available for class '" + clazz.getName() + "'");
        }

        return serializer;
    }

    /**
     * Tries to get the serializer for the given class
     *
     * @param clazz The class to obtain a serializer for
     * @return An empty optional if no serializer is registered
     */
    public static <T> Optional<Kodeck<T>> maybeGet(Class<T> clazz) {
        return Optional.ofNullable(getOrNull(clazz));
    }

    @SuppressWarnings("unchecked")
    private static <T> @Nullable Kodeck<T> getOrNull(Class<T> clazz) {
        Kodeck<T> serializer = (Kodeck<T>) SERIALIZERS.get(clazz);

        if (serializer == null) {
            if (Record.class.isAssignableFrom(clazz))
                serializer = (Kodeck<T>) ReflectionKodeckBuilder.createRecordSerializer(conform(clazz, Record.class));
            else if (clazz.isEnum())
                serializer = (Kodeck<T>) ReflectionKodeckBuilder.createEnumSerializer(conform(clazz, Enum.class));
            else if (clazz.isArray())
                serializer = (Kodeck<T>) ReflectionKodeckBuilder.createArraySerializer(clazz.getComponentType());
            else if (clazz.isAnnotationPresent(SealedPolymorphic.class))
                serializer = (Kodeck<T>) ReflectionKodeckBuilder.createSealedSerializer(clazz);
            else
                return null;

            SERIALIZERS.put(clazz, serializer);
        }


        return serializer;
    }

    /**
     * Registers a supplier that creates empty collections for the
     * map and collection serializers to use
     *
     * @param clazz    The container class to register a provider for
     * @param provider A provider that creates some default type for the given
     *                 class
     */
    public static <T> void registerCollectionProvider(Class<T> clazz, Supplier<T> provider) {
        if (COLLECTION_PROVIDERS.containsKey(clazz)) throw new IllegalStateException("Collection class '" + clazz.getName() + "' already has a provider");
        COLLECTION_PROVIDERS.put(clazz, provider);
    }

    /**
     * Creates a new collection instance
     * for the given container class
     *
     * @param clazz The container class
     * @return The created collection
     */
    public static <T> T createCollection(Class<? extends T> clazz) {
        if (!COLLECTION_PROVIDERS.containsKey(clazz)) {
            throw new IllegalStateException("No collection provider registered for collection class " + clazz.getName());
        }

        //noinspection unchecked
        return ((Supplier<T>) COLLECTION_PROVIDERS.get(clazz)).get();
    }

    /**
     * Tries to create a serializer capable of
     * serializing the given map type
     *
     * @param clazz      The map type
     * @param keyClass   The type of the map's keys
     * @param valueClass The type of the map's values
     * @return The created serializer
     */
    public static <K, V, T extends Map<K, V>> Kodeck<T> createMapSerializer(Class<T> clazz, Class<K> keyClass, Class<V> valueClass) {
        createCollection(clazz);

        var keyKodeck = get(keyClass);
        var valueKodeck = get(valueClass);

        return keyKodeck == Kodeck.STRING
                ? (Kodeck<T>) valueKodeck.map()
                : (Kodeck<T>) Kodeck.mapOf(keyKodeck, valueKodeck);
    }

    /**
     * Tries to create a serializer capable of
     * serializing the given collection type
     *
     * @param clazz        The collection type
     * @param elementClass The type of the collections elements
     * @return The created serializer
     */
    public static <E, T extends Collection<E>> Kodeck<T> createCollectionSerializer(Class<T> clazz, Class<E> elementClass) {
        createCollection(clazz);

        var elementKodeck = get(elementClass);

        return elementKodeck.list()
                .then(es -> {
                    T collection = createCollection(clazz);

                    collection.addAll(es);

                    return collection;
                }, List::copyOf);
    }

    /**
     * Tries to create a serializer capable of
     * serializing optionals with the given element type
     *
     * @param elementClass The type of the collections elements
     * @return The created serializer
     */
    public static <E> Kodeck<Optional<E>> createOptionalSerializer(Class<E> elementClass) {
        var elementKodeck = get(elementClass);

        return elementKodeck.optionalOf();
    }

    /**
     * Tries to create a serializer capable of
     * serializing arrays of the given element type
     *
     * @param elementClass The array element type
     * @return The created serializer
     */
    @SuppressWarnings("unchecked")
    public static Kodeck<?> createArraySerializer(Class<?> elementClass) {
        var elementSerializer = (Kodeck<Object>) get(elementClass);

        return elementSerializer.list().then(list -> {
            final int length = list.size();
            Object array = Array.newInstance(elementClass, length);
            for (int i = 0; i < length; i++) {
                Array.set(array, i, list.get(i));
            }
            return array;
        }, t -> {
            final int length = Array.getLength(t);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Array.get(t, i));
            }
            return list;
        });
    }

    /**
     * Tries to create a serializer capable of
     * serializing the given record class
     *
     * @param clazz The class to create a serializer for
     * @return The created serializer
     */
    public static <R extends Record> Kodeck<R> createRecordSerializer(Class<R> clazz) {
        return RecordKodeck.create(clazz);
    }

    /**
     * Tries to create a serializer capable of serializing
     * the given enum type
     *
     * @param enumClass The type of enum to create a serializer for
     * @return The created serializer
     */
    public static <E extends Enum<E>> Kodeck<E> createEnumSerializer(Class<E> enumClass) {
        return Kodeck.INT.then(integer -> enumClass.getEnumConstants()[integer], Enum::ordinal);
    }

    @SuppressWarnings("unchecked")
    public static <T, K> Kodeck<T> createDispatchedSerializer(Function<K, Kodeck<? extends T>> keyToKodeck, Function<T, K> keyGetter, Kodeck<K> keyKodeck) {
        return new Kodeck<>() {
            @Override
            public <E> T decode(Format<E> ops, E object) {
                MutableObject<K> key = new MutableObject<>();
                MutableObject<T> value = new MutableObject<>();

                ops.getStringBasedMap(object).forEach(entry -> {
                    if (Objects.equals(entry.getKey(), "present")) {
                        key.setValue(keyKodeck.decode(ops, entry.getValue()));
                    } else if (Objects.equals(entry.getKey(), "value")) {
                        var kodeck = keyToKodeck.apply(key.getValue());

                        value.setValue(kodeck.decode(ops, entry.getValue()));
                    }
                });

                return value.getValue();
            }

            @Override
            public <E> E encode(Format<E> ops, T object, E prefix) {
                E map = ops.createStringBasedMap(2, prefix);

                var key = keyGetter.apply(object);
                Kodeck<T> kodeck = (Kodeck<T>) keyToKodeck.apply(key);

                ops.addMapEntry("key", () -> keyKodeck.encode(ops, key, prefix), map);
                ops.addMapEntry("value", () -> kodeck.encode(ops, object, prefix), map);

                return map;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Kodeck<?> createSealedSerializer(Class<?> commonClass) {
        if (!commonClass.isSealed())
            throw new IllegalStateException("@SealedPolymorphic class should be sealed!");

        List<Class<?>> sortedPermittedSubclasses = Arrays.stream(commonClass.getPermittedSubclasses()).collect(Collectors.toList());

        for (int i = 0; i < sortedPermittedSubclasses.size(); i++) {
            Class<?> klass = sortedPermittedSubclasses.get(i);

            if (klass.isSealed()) {
                for (Class<?> subclass : klass.getPermittedSubclasses()) {
                    if (!sortedPermittedSubclasses.contains(subclass))
                        sortedPermittedSubclasses.add(subclass);
                }
            }
        }

        for (Class<?> klass : sortedPermittedSubclasses) {
            if (!klass.isSealed() && !Modifier.isFinal(klass.getModifiers()))
                throw new IllegalStateException("Subclasses of a @SealedPolymorphic class must be sealed themselves!");
        }

        sortedPermittedSubclasses.sort(Comparator.comparing(Class::getName));

        Int2ObjectMap<Kodeck<?>> serializerMap = new Int2ObjectOpenHashMap<>();
        Reference2IntMap<Class<?>> classesMap = new Reference2IntOpenHashMap<>();

        classesMap.defaultReturnValue(-1);

        for (int i = 0; i < sortedPermittedSubclasses.size(); i++) {
            Class<?> klass = sortedPermittedSubclasses.get(i);

            serializerMap.put(i, ReflectionKodeckBuilder.get(klass));
            classesMap.put(klass, i);
        }

        return CascadeKodeck.dispatchedOf(serializerMap::get, v -> classesMap.getInt(v.getClass()), Kodeck.INT);
    }

    @SuppressWarnings("unchecked")
    private static <T> Class<T> conform(Class<?> clazz, Class<T> target) {
        return (Class<T>) clazz;
    }

    static {

        // ----------
        // Primitives
        // ----------

        register(Kodeck.BOOLEAN, Boolean.class, boolean.class);
        register(Kodeck.INT, Integer.class, int.class);
        register(Kodeck.LONG, Long.class, long.class);
        register(Kodeck.FLOAT, Float.class, float.class);
        register(Kodeck.DOUBLE, Double.class, double.class);

        register(Kodeck.BYTE, Byte.class, byte.class);
        register(Kodeck.SHORT, Short.class, short.class);
        register(Kodeck.SHORT.then(aShort -> (char) aShort.shortValue(), character -> (short) character.charValue()), Character.class, char.class);

        register(Void.class, Kodeck.EMPTY);

        // ----
        // Misc
        // ----

        register(String.class, Kodeck.STRING);
        register(UUID.class, Kodeck.UUID);
        register(Date.class, Kodeck.DATE);
        register(PacketByteBuf.class, Kodeck.PACKET_BYTE_BUF);

        // --------
        // MC Types
        // --------

        register(BlockPos.class, Kodeck.BLOCK_POS);
        register(ChunkPos.class, Kodeck.CHUNK_POS);
        register(ItemStack.class, Kodeck.ITEM_STACK);
        register(Identifier.class, Kodeck.IDENTIFIER);
        register(NbtCompound.class, Kodeck.COMPOUND);
        register(BlockHitResult.class, PacketByteBuf::writeBlockHitResult, PacketByteBuf::readBlockHitResult);
        register(BitSet.class, Kodeck.BITSET);
        register(Text.class, Kodeck.TEXT);

//        register(ParticleEffect.class,
//                Kodeck.STRING.then(s -> {
//                    try {
//                        return ParticleEffectArgumentType.readParameters(new StringReader(s), Registries.PARTICLE_TYPE.getReadOnlyWrapper());
//                    } catch (CommandSyntaxException ignore) {
//                        throw new IllegalStateException("Issue was found when trying to read ParticleType!");
//                    }
//                }, ParticleEffect::asString)
//        );

        register(ParticleEffect.class,
                Kodeck.PACKET_BYTE_BUF.then(
                        byteBuf -> {
                            //noinspection rawtypes
                            final ParticleType particleType = Registries.PARTICLE_TYPE.get(byteBuf.readInt());
                            //noinspection unchecked, ConstantConditions

                            return particleType.getParametersFactory().read(particleType, byteBuf);
                        },
                        particleEffect -> {
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeInt(Registries.PARTICLE_TYPE.getRawId(particleEffect.getType()));
                            particleEffect.write(buf);

                            return buf;
                        }
                )
        );

        register(Vec3d.class, Kodeck.DOUBLE.list()
                .then(
                        doubles -> new Vec3d(doubles.get(0), doubles.get(1), doubles.get(2)),
                        vec3d -> List.of(vec3d.getX(), vec3d.getY(), vec3d.getZ())
                ));

        register(Vector3f.class, Kodeck.FLOAT.list()
                .then(
                        doubles -> new Vector3f(doubles.get(0), doubles.get(1), doubles.get(2)),
                        vec3d -> List.of(vec3d.x(), vec3d.y(), vec3d.z())
                ));

        // -----------
        // Collections
        // -----------

        registerCollectionProvider(Collection.class, HashSet::new);
        registerCollectionProvider(List.class, ArrayList::new);
        registerCollectionProvider(Map.class, LinkedHashMap::new);
    }
}
