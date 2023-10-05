package io.wispforest.owo.serialization;

import io.wispforest.owo.serialization.impl.RecursiveLogger;
import io.wispforest.owo.serialization.impl.kodecks.FormatKodeck;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface StructuredFormat<E, M extends E> extends Format<E> {

    FormatKodeck<E> getFormatKodeck();

    @Override
    default E createSequence(E prefix){
        return this.createStringBasedMap(0, empty());
    }

    @Override
    default E addSequenceEntry(String key, E entry, E prefix){
        put((M) prefix, key, entry);

        return prefix;
    }

    @Override
    default E getSequenceEntry(String key, E input){
        return this.get((M) input, key);
    }

    //--

    E get(M map, String key);

    E put(M map, String key, E value);

    E delete(M map, String key);

    boolean contains(M map, String key);


}
