package io.wispforest.owo.serialization;

import java.util.Optional;
import java.util.function.Function;

public interface CascadeKodeck<T, K> extends SequenceKodeck<T> {

    static <T> Kodeck<Optional<T>> optionalOf(Kodeck<T> kodeck){
        return new CascadeKodeck<Optional<T>, Boolean>() {

            @Override
            public Boolean getKey(Optional<T> object) { return object.isPresent(); }

            @Override
            public Kodeck<Boolean> keyKodeck() { return Kodeck.BOOLEAN; }

            @Override
            public <E> void valueEncode(SequenceHandler<E> handler, Optional<T> object, Boolean key) {
                if(key) handler.encodeEntry("value", kodeck, object.get());
            }

            @Override
            public <E> Optional<T> valueDecode(SequenceHandler<E> handler, Boolean key) {
                return Optional.ofNullable(key ? handler.decodeEntry("value", kodeck) : null);
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
            public <E> void valueEncode(SequenceHandler<E> handler, T object, K key) {
                handler.encodeEntry("value", ((Kodeck<T>) keyToKodeck.apply(key)), object);
            }

            @Override
            public <E> T valueDecode(SequenceHandler<E> handler, K key) {
                return handler.decodeEntry("value", keyToKodeck.apply(key));
            }
        };
    }

    @Override
    default <E> void encodeObject(SequenceHandler<E> handler, T object){
        K key = getKey(object);

        handler.encodeEntry("key", keyKodeck(), getKey(object));
        valueEncode(handler, object, key);
    }

    @Override
    default <E> T decodeObject(SequenceHandler<E> handler){
        K key = handler.decodeEntry("key", keyKodeck());

        return valueDecode(handler, key);
    }

    K getKey(T object);

    Kodeck<K> keyKodeck();

    <E> void valueEncode(SequenceHandler<E> handler, T object, K key);

    <E> T valueDecode(SequenceHandler<E> handler, K key);
}
