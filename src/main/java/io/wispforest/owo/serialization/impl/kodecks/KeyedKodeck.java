package io.wispforest.owo.serialization.impl.kodecks;

import io.wispforest.owo.serialization.Format;
import io.wispforest.owo.serialization.Kodeck;
import io.wispforest.owo.serialization.StructuredFormat;
import org.jetbrains.annotations.NotNull;

public class KeyedKodeck<T> implements Kodeck<T> {

    private final String key;
    private final Kodeck<T> kodeck;

    public KeyedKodeck(String key, Kodeck<T> kodeck){
        this.key = key;
        this.kodeck = kodeck;
    }

    public <E, M extends E, F extends StructuredFormat<E, M>> T get(F format, @NotNull M map){
        return kodeck.decode(format, format.get(map, key));
    }

    public <E, M extends E, F extends StructuredFormat<E, M>> void put(F format, @NotNull M map, T value){
        format.put(map, key, kodeck.encode(format, value));
    }

    public <E, M extends E, F extends StructuredFormat<E, M>> void delete(F format, @NotNull M map){
        format.delete(map, key);
    }

    public <E, M extends E, F extends StructuredFormat<E, M>> boolean isIn(F format, @NotNull M map){
        return format.contains(map, key);
    }

    @Override
    public <E> T decode(Format<E> format, E object) {
        if(!(format instanceof StructuredFormat<E, ?> sformat)) {
            throw new IllegalStateException("Unable to use KeyedKodeck on formats other than StructuredFormat's!");
        }

        E map = (E) sformat.getFormatKodeck().decode(format, object);

        return kodeck.decode(sformat, ((StructuredFormat<E, E>) sformat).get(map, key));
    }

    @Override
    public <E> E encode(Format<E> format, T object, E prefix) {
        if(!(format instanceof StructuredFormat<E, ?> sformat)) {
            throw new IllegalStateException("Unable to use KeyedKodeck on formats other than StructuredFormat's!");
        }

        E map = sformat.createStringBasedMap(1, prefix);

        return sformat.addMapEntry(key, () -> kodeck.encode(sformat, object), map);
    }
}
