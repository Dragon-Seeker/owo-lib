package io.wispforest.owo.serialization;

import io.wispforest.owo.serialization.impl.kodecks.FormatKodeck;

public interface StructuredFormat<E, M extends E> extends Format<E> {

    FormatKodeck<E> getFormatKodeck();

    //--

    E get(M map, String key);

    E put(M map, String key, E value);

    E delete(M map, String key);

    boolean contains(M map, String key);


}
