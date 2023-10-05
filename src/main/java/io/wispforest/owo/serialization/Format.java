package io.wispforest.owo.serialization;

import com.google.gson.JsonElement;
import org.apache.commons.lang3.function.TriFunction;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface Format<T> {

    //--

    T empty();

    T createBoolean(final boolean value, T prefix);

    T createByte(final byte value, T prefix);

    T createShort(final short value, T prefix);

    T createInt(final int value, T prefix);

    T createLong(final long value, T prefix);

    T createFloat(final float value, T prefix);

    T createDouble(final double value, T prefix);

    T createString(final String value, T prefix);

    T createStringBasedMap(final int size, T prefix);

    T addMapEntry(String key, Supplier<T> input, T prefix);

    T createList(final int size, T prefix);

    T addListEntry(T input, T prefix);

    T createSequence(T prefix);

    T addSequenceEntry(String key, T entry, T prefix);

    //----

    boolean getBoolean(T input);

    byte getByte(T input);

    short getShort(T input);

    int getInt(T input);

    long getLong(T input);

    float getFloat(T input);

    double getDouble(T input);

    String getString(T input);

    Stream<Map.Entry<String, T>> getStringBasedMap(T input);

    Stream<T> getList(T input);

    T getSequenceEntry(String key, T input);

}
