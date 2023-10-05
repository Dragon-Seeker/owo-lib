package io.wispforest.owo.serialization.reflect;

import com.google.common.collect.ImmutableMap;
import io.wispforest.owo.Owo;
import io.wispforest.owo.serialization.Format;
import io.wispforest.owo.serialization.Kodeck;
import io.wispforest.owo.serialization.SequenceKodeck;
import org.apache.commons.lang3.mutable.MutableInt;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RecordKodeck<R extends Record> implements SequenceKodeck<R> {
    private static final Map<Class<?>, RecordKodeck<?>> SERIALIZERS = new HashMap<>();

    private final Map<String, RecordEntryHandler<R>> adapters;
    private final Class<R> recordClass;
    private final Constructor<R> instanceCreator;
    private final int fieldCount;

    private RecordKodeck(Class<R> recordClass, Constructor<R> instanceCreator, ImmutableMap<String, RecordEntryHandler<R>> adapters) {
        this.recordClass = recordClass;
        this.instanceCreator = instanceCreator;
        this.adapters = adapters;
        this.fieldCount = recordClass.getRecordComponents().length;
    }

    /**
     * Creates a new serializer for the given record type, or retrieves the
     * existing one if it was already created
     *
     * @param recordClass The type of record to (de-)serialize
     * @param <R>         The type of record to (de-)serialize
     * @return The serializer for the given record type
     */
    public static <R extends Record> RecordKodeck<R> create(Class<R> recordClass) {
        if (SERIALIZERS.containsKey(recordClass)) return (RecordKodeck<R>) SERIALIZERS.get(recordClass);

        final ImmutableMap.Builder<String, RecordEntryHandler<R>> handlerBuilder = new ImmutableMap.Builder<>();

        final Class<?>[] canonicalConstructorArgs = new Class<?>[recordClass.getRecordComponents().length];

        var lookup = MethodHandles.publicLookup();
        for (int i = 0; i < recordClass.getRecordComponents().length; i++) {
            try {
                var component = recordClass.getRecordComponents()[i];
                var handle = lookup.unreflect(component.getAccessor());

                handlerBuilder.put(component.getName(),
                        new RecordEntryHandler<>(
                                r -> getRecordEntry(r, handle),
                                ReflectionKodeckBuilder.getGeneric(component.getGenericType())
                        )
                );

                canonicalConstructorArgs[i] = component.getType();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not create method handle for record component");
            }
        }

        try {
            final var serializer = new RecordKodeck<>(recordClass, recordClass.getConstructor(canonicalConstructorArgs), handlerBuilder.build());
            SERIALIZERS.put(recordClass, serializer);
            return serializer;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not locate canonical record constructor");
        }
    }

    public Class<R> getRecordClass() {
        return recordClass;
    }

    private static <R extends Record> Object getRecordEntry(R instance, MethodHandle accessor) {
        try {
            return accessor.invoke(instance);
        } catch (Throwable e) {
            throw new IllegalStateException("Unable to get record component value", e);
        }
    }

    /**
     * Attempts to read a record of this serializer's
     * type from the given buffer
     *
     * @return The deserialized record
     */
    @Override
    public <E> R decodeObject(SequenceHandler<E> handler) {
        Object[] messageContents = new Object[fieldCount];

        var index = new MutableInt();

        adapters.forEach((s, fHandler) -> {
            messageContents[index.getAndIncrement()] = handler.decodeEntry(s, fHandler.kodeck);
        });

        try {
            return instanceCreator.newInstance(messageContents);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            Owo.LOGGER.error("Error while deserializing record", e);
        }

        return null;
    }

    /**
     * Writes the given record instance
     * to the given buffer
     *
     * @param instance The record instance to serialize
     */
    @Override
    public <E> void encodeObject(SequenceHandler<E> handler, R instance) {
        adapters.forEach((s, fHandler) -> handler.encodeEntry(s, fHandler.kodeck, fHandler.rFunction.apply(instance)));
    }

    private record RecordEntryHandler<R>(Function<R, ?> rFunction, Kodeck kodeck) { }
}
