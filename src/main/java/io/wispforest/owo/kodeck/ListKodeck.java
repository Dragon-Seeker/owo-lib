package io.wispforest.owo.kodeck;

import org.apache.commons.lang3.function.TriFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public interface ListKodeck<V> extends Kodeck<List<V>> {

    <E> V entryDecode(Format<E> ops, E object);

    <E> E entryEncode(Format<E> ops, V object, E prefix);

    default <R> ListKodeck<R> entryThen(Function<V, R> getter, Function<R, V> setter) {
        return new ListKodeck<R>() {
            @Override
            public <E> R entryDecode(Format<E> ops, E object) {
                return getter.apply(ListKodeck.this.entryDecode(ops, object));
            }

            @Override
            public <E> E entryEncode(Format<E> ops, R object, E prefix) {
                return ListKodeck.this.entryEncode(ops, setter.apply(object), prefix);
            }
        };
    }

    static <V> ListKodeck<V> of(Kodeck<V> kodeck) {
        return new ListKodeck<V>() {
            @Override
            public <E> V entryDecode(Format<E> ops, E object) {
                return kodeck.decode(ops, object);
            }

            @Override
            public <E> E entryEncode(Format<E> ops, V object, E prefix) {
                return kodeck.encode(ops, object, prefix);
            }
        };
    }

    @Override
    default <E> List<V> decode(Format<E> ops, E object) {
        return ops.getList(object).map(e -> entryDecode(ops, e)).toList();
    }

    @Override
    default <E> E encode(Format<E> ops, List<V> object, E prefix) {
        var listData = ops.createList(object.size(), prefix);

        for (V v : object) {
            ops.addListEntry(entryEncode(ops, v, prefix), listData);
        }

        return listData;
    }

    static <E, V> List<V> getList(BiFunction<Format<E>, E, V> decodeFunc, Format<E> ops, E object){
        return ops.getList(object).map(e -> decodeFunc.apply(ops, e)).toList();
    }


    static <V, T> T handleList(TriFunction<Format<T>, V, T, T> encodeFunc, Format<T> ops, List<V> object, T prefix){
        var listData = ops.createList(object.size(), prefix);

        for (V v : object) {
            ops.addListEntry(encodeFunc.apply(ops, v, prefix), listData);
        }

        return listData;
    }
}
