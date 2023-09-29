package io.wispforest.owo.kodeck;

import org.apache.commons.lang3.function.TriFunction;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface MapKodeck<K, V> extends Kodeck<Map<K, V>> {
    static <V> MapKodeck<String, V> of(Kodeck<V> valueKodeck) {
        return new MapKodeck<>() {
            @Override
            public String toKey(String object) {
                return object;
            }

            @Override
            public String fromKey(String object) {
                return object;
            }

            @Override
            public <E> V valueDecode(Format<E> ops, E object) {
                return valueKodeck.decode(ops, object);
            }

            @Override
            public <E> E valueEncode(Format<E> ops, V object, E prefix) {
                return valueKodeck.encode(ops, object, prefix);
            }
        };
    }

    K toKey(String object);

    String fromKey(K object);

    <E> V valueDecode(Format<E> ops, E object);

    <E> E valueEncode(Format<E> ops, V object, E prefix);

    //---

    default <R> MapKodeck<R, V> keyThen(Function<K, R> toFunc, Function<R, K> fromFunc) {
        return new MapKodeck<>() {
            @Override
            public R toKey(String object) {
                return toFunc.apply(MapKodeck.this.toKey(object));
            }

            @Override
            public String fromKey(R object) {
                return MapKodeck.this.fromKey(fromFunc.apply(object));
            }

            @Override
            public <E> V valueDecode(Format<E> ops, E object) {
                return MapKodeck.this.valueDecode(ops, object);
            }

            @Override
            public <E> E valueEncode(Format<E> ops, V object, E prefix) {
                return MapKodeck.this.valueEncode(ops, object, prefix);
            }
        };
    }

    default <R> MapKodeck<K, R> valueThen(Function<V, R> toFunc, Function<R, V> fromFunc) {
        return new MapKodeck<>() {
            @Override
            public K toKey(String object) {
                return MapKodeck.this.toKey(object);
            }

            @Override
            public String fromKey(K object) {
                return MapKodeck.this.fromKey(object);
            }

            @Override
            public <E> R valueDecode(Format<E> ops, E object) {
                return toFunc.apply(MapKodeck.this.valueDecode(ops, object));
            }

            @Override
            public <E> E valueEncode(Format<E> ops, R object, E prefix) {
                return MapKodeck.this.valueEncode(ops, fromFunc.apply(object), prefix);
            }
        };
    }

    //---

    @Override
    default <E> Map<K, V> decode(Format<E> ops, E object) {
        Map<K, V> decodeMap = new HashMap<>();

        ops.getStringBasedMap(object).forEach((entry) -> {
            var key = entry.getKey();
            var value = entry.getValue();

            decodeMap.put(toKey(key), valueDecode(ops, value));
        });

        return decodeMap;
    }

    @Override
    default <E> E encode(Format<E> ops, Map<K, V> object, E prefix) {
        E mapElement = ops.createStringBasedMap(object.size(), prefix);

        object.forEach((k, v) -> {
            ops.addMapEntry(fromKey(k), () -> valueEncode(ops, v, prefix), mapElement);
        });

        return mapElement;
    }

    static <E, V> Map<String, V> getMap(BiFunction<Format<E>, E, V> decodeFunc, Format<E> ops, E object){
        return getMap(s -> s, decodeFunc, ops, object);
    }

    static <E, K, V> Map<K, V> getMap(Function<String, K> toKeyFunc, BiFunction<Format<E>, E, V> decodeFunc, Format<E> ops, E object){
        Map<K, V> decodeMap = new HashMap<>();

        ops.getStringBasedMap(object).forEach((entry) -> {
            var key = entry.getKey();
            var value = entry.getValue();

            decodeMap.put(toKeyFunc.apply(key), decodeFunc.apply(ops, value));
        });

        return decodeMap;
    }

    static <E, V> E handleMap(TriFunction<Format<E>, V, E, E> encodeFunc, Format<E> ops, Map<String, V> object, E prefix) {
        return handleMap(s -> s, encodeFunc, ops, object, prefix);
    }

    static <E, K, V> E handleMap(Function<K, String> fromKey, TriFunction<Format<E>, V, E, E> encodeFunc, Format<E> ops, Map<K, V> object, E prefix) {
        E mapElement = ops.createStringBasedMap(object.size(), prefix);

        object.forEach((k, v) -> {
            ops.addMapEntry(fromKey.apply(k), () -> encodeFunc.apply(ops, v, prefix), mapElement);
        });

        return mapElement;
    }
}
