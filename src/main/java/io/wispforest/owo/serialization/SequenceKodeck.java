package io.wispforest.owo.serialization;

import io.wispforest.owo.serialization.impl.RecursiveLogger;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;

public interface SequenceKodeck<T> extends Kodeck<T>{

    static <L, R> Kodeck<Pair<L, R>> pairOf(Kodeck<L> leftKodeck, Kodeck<R> rightKodeck){
        return new SequenceKodeck<>() {
            @Override
            public <E> void encodeObject(SequenceHandler<E> handler, Pair<L, R> object) {
                handler.encodeEntry("l", leftKodeck, object.getLeft());
                handler.encodeEntry("r", rightKodeck, object.getRight());
            }

            @Override
            public <E> Pair<L, R> decodeObject(SequenceHandler<E> handler) {
                var pair = new Pair<L, R>(null, null);

                pair.setLeft(handler.decodeEntry("l", leftKodeck));
                pair.setRight(handler.decodeEntry("r", rightKodeck));

                return pair;
            }
        };
    }

    <E> void encodeObject(SequenceHandler<E> handler, T object);

    <E> T decodeObject(SequenceHandler<E> handler);

    @Override
    default <E> T decode(Format<E> ops, E object) {
        SequenceHandler<E> handler = new SequenceHandler<>(ops, object, null);

        return decodeObject(handler);
    }

    @Override
    default <E> E encode(Format<E> ops, T object, E prefix) {
        RecursiveLogger.DataAccessHelper helper = null;

        if(ops instanceof RecursiveLogger logger){
            helper = logger.makeDataAccessHelper("sequence", ArrayList::new);
        }

        var e = ops.createSequence(prefix);

        SequenceHandler<E> handler = new SequenceHandler<>(ops, prefix, e);

        encodeObject(handler, object);

        if(helper != null) helper.pop();

        return e;
    }

    class SequenceHandler<E> {
        public final Format<E> format;

        public final E initialPrefix;
        private final E sequencePrefix;

        private SequenceHandler(Format<E> format, E initialPrefix, E sequencePrefix){
            this.format = format;

            this.initialPrefix = initialPrefix;
            this.sequencePrefix = sequencePrefix;
        }

        public <V> void encodeEntry(String key, Kodeck<V> kodeck, V value){
            format.addSequenceEntry(key, kodeck.encode(format, value, initialPrefix), sequencePrefix);
        }

        public <V> V decodeEntry(String key, Kodeck<V> kodeck){
            return kodeck.decode(format, format.getSequenceEntry(key, initialPrefix));
        }
    }
}
