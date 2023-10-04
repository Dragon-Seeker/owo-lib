package io.wispforest.owo.serialization.impl.kodecks;

import io.wispforest.owo.serialization.*;

import java.util.*;
import java.util.function.Function;

public class FormatKodeck<T> implements Kodeck<T> {

    private final Format<T> format;

    private final Map<Byte, Kodeck<?>> dataTypeHandlers = new HashMap<>();
    private final Function<T, Byte> dataTypeGetter;

    public FormatKodeck(StructuredFormat<T, ?> format, Function<T, Byte> dataTypeGetter){
        this(format, (DataTypeHandlersFactory<T>) DataTypeHandlersFactory.DEFAULT, dataTypeGetter);
    }

    public FormatKodeck(Format<T> format, DataTypeHandlersFactory<T> init, Function<T, Byte> dataTypeGetter){
        this.format = format;
        this.dataTypeGetter = dataTypeGetter;

        init.initData(this);
    }

    public FormatKodeck<T> addToMap(byte dataType, Kodeck<?> kodeck){
        if(dataType < 0){
            throw new IllegalStateException("Byte for the given dataType add call has a -1 number which is not allowed!");
        }

        if(dataTypeHandlers.containsKey(dataType)) {
            throw new IllegalStateException("Attempted Double Register of datatype for a FormatKodeck! [Byte: " + dataType + "]");
        }

        dataTypeHandlers.put(dataType, kodeck);

        return this;
    }

    @Override
    public <E> T decode(Format<E> ops, E object) {
        var inPrefix = format.empty();

        if(ops.getClass().isInstance(format)){
            return (T) object;
        } else if(ops instanceof StructuredFormat<E, ?> outFormat){
            return outFormat.getFormatKodeck().encode(format, (E) object, format.empty());
        }

        byte dataType = ops.getByte(object);

        if(!dataTypeHandlers.containsKey(dataType)){
            throw new IllegalStateException("Unable to process a given dataType due to the format not supporting such!");
        }

        Kodeck<T> kodeck = (Kodeck<T>) dataTypeHandlers.get(dataType);

        if(kodeck == null) return format.empty();

        return kodeck.encode(format, kodeck.decode(ops, object), inPrefix);
    }

    @Override
    public <E> E encode(Format<E> ops, T object, E prefix) {
        if (ops.getClass().isInstance(format)) return (E) object;

        byte dataType = dataTypeGetter.apply(object);

        if(!(ops instanceof StructuredFormat<E, ?>)) ops.createByte(dataType, prefix);

        if(!dataTypeHandlers.containsKey(dataType)){
            throw new IllegalStateException("Unable to process a given dataType due to the format not supporting such!");
        }

        Kodeck<T> kodeck = (Kodeck<T>) dataTypeHandlers.get(dataType);

        if(kodeck == null){
            return ops.empty();
        }

        return kodeck.encode(ops, kodeck.decode(format, object), prefix);
    }

    public interface DataTypeHandlersFactory<T> {
        DataTypeHandlersFactory<?> DEFAULT = kodeck -> {
            kodeck.addToMap((byte) 0, null)
                    .addToMap((byte) 1, Kodeck.BOOLEAN)
                    .addToMap((byte) 2, Kodeck.BYTE)
                    .addToMap((byte) 3, Kodeck.SHORT)
                    .addToMap((byte) 4, Kodeck.INT)
                    .addToMap((byte) 5, Kodeck.LONG)
                    .addToMap((byte) 6, Kodeck.FLOAT)
                    .addToMap((byte) 7, Kodeck.DOUBLE)
                    .addToMap((byte) 8, Kodeck.STRING)
                    .addToMap((byte) 9, ListKodeck.of(kodeck))
                    .addToMap((byte) 10, MapKodeck.of(kodeck));
        };

        void initData(FormatKodeck<T> kodeck);
    }
}
