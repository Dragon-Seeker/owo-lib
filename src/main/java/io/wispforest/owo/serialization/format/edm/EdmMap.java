package io.wispforest.owo.serialization.format.edm;

import io.wispforest.owo.serialization.SerializationContext;
import io.wispforest.owo.serialization.endec.KeyedEndec;
import io.wispforest.owo.serialization.util.MapCarrier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class EdmMap extends EdmElement<Map<String, EdmElement<?>>> implements MapCarrier {

    EdmMap(EdmElement<Map<String, EdmElement<?>>> mapElement) {
        super(mapElement.value(), mapElement.type());
    }

    @Override
    public <T> T getWithErrors(@NotNull KeyedEndec<T> key, SerializationContext ctx) {
        if (!this.has(key)) return key.defaultValue();
        return key.endec().decodeFully(ctx, EdmDeserializer::of, this.value().get(key.key()));
    }

    @Override
    public <T> void put(@NotNull KeyedEndec<T> key, @NotNull T value, SerializationContext ctx) {
        this.value().put(key.key(), key.endec().encodeFully(ctx, EdmSerializer::of, value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.value().remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.value().containsKey(key.key());
    }
}