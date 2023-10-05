package io.wispforest.owo.serialization;

import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Similar to pair but requires that the key value is handled first and then passed down
 * to handle the main object of the given Primary type
 * @param <T>
 * @param <K>
 */
public interface CascadeKodeck<T, K> extends Kodeck<T> {

    static <T> Kodeck<Optional<T>> optionalOf(Kodeck<T> kodeck){
        return new CascadeKodeck<Optional<T>, Boolean>() {

            @Override
            public Boolean getKey(Optional<T> object) { return object.isPresent(); }

            @Override
            public Kodeck<Boolean> keyKodeck() { return Kodeck.BOOLEAN; }

            @Override
            public <E> E valueEncode(Format<E> ops, Optional<T> object, E prefix, Boolean key) {
                return key ? kodeck.encode(ops, object.get(), prefix) : ops.empty();
            }

            @Override
            public <E> Optional<T> valueDecode(Format<E> ops, E object, Boolean key) {
                return Optional.ofNullable(key ? kodeck.decode(ops, object) : null);
            }
        };
    }

    static <T, K> Kodeck<T> dispatchedOf(Function<K, Kodeck<? extends T>> keyToKodeck, Function<T, K> keyGetter, Kodeck<K> keyKodeck) {
        return new CascadeKodeck<T, K>() {

            @Override
            public K getKey(T object) { return keyGetter.apply(object); }

            @Override
            public Kodeck<K> keyKodeck() { return keyKodeck; }

            @Override
            public <E> E valueEncode(Format<E> ops, T object, E prefix, K key) {
                return ((Kodeck<T>) keyToKodeck.apply(key)).encode(ops, object, prefix);
            }

            @Override
            public <E> T valueDecode(Format<E> ops, E object, K key) {
                return keyToKodeck.apply(key).decode(ops, object);
            }
        };
    }

    K getKey(T object);

    Kodeck<K> keyKodeck();

    <E> E valueEncode(Format<E> ops, T object, E prefix, K key);

    <E> T valueDecode(Format<E> ops, E object, K key);

    //--

    @Override
    default <E> T decode(Format<E> ops, E object) {
        MutableObject<K> keyValue = new MutableObject<>();
        MutableObject<T> value = new MutableObject<>(null);

        ops.getStringBasedMap(object).forEach(entry -> {
            if (Objects.equals(entry.getKey(), "key")) {
                keyValue.setValue(keyDecode(ops, entry.getValue()));
            } else if (Objects.equals(entry.getKey(), "value")) {
                value.setValue(valueDecode(ops, entry.getValue(), keyValue.getValue()));
            }
        });

        return value.getValue();
    }

    @Override
    default <E> E encode(Format<E> ops, T object, E prefix) {
        E map = ops.createStringBasedMap(2, prefix);

        var key = getKey(object);

        ops.addMapEntry("key", () -> keyEncode(ops, key, prefix), map);
        ops.addMapEntry("value", () -> valueEncode(ops, object, prefix, key), map);

        return map;
    }

    default <E> E keyEncode(Format<E> ops, K object, E prefix){
        return keyKodeck().encode(ops, object, prefix);
    }

    default <E> K keyDecode(Format<E> ops, E object){
        return keyKodeck().decode(ops, object);
    }

    //--




}
