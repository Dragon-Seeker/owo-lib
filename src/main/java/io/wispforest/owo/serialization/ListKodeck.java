package io.wispforest.owo.serialization;

import java.util.List;
import java.util.function.Function;

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
}
