package io.wispforest.owo.serialization.impl.kodecks;

import com.mojang.datafixers.types.Func;
import io.wispforest.owo.serialization.*;
import org.apache.commons.lang3.mutable.MutableBoolean;

import java.util.*;
import java.util.function.Function;

public class FormatKodeck<T> implements Kodeck<T> {

    private final Format<T> format;

    private final Map<Byte, Kodeck<?>> dataTypeHandlers = new HashMap<>();
    private final Function<T, Byte> dataTypeGetter;

    private final Map<Byte, Function<Object, Collection<Object>>> dataReducible = new HashMap<>();

    private final Map<Object, MutableBoolean> encodeByteLocks = new IdentityHashMap();
    private final Deque<Byte> decodeDataTypeStack = new ArrayDeque(List.of((byte)-1));

    private boolean dataSaverMode = false;

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
            throw new IllegalStateException("Byte for the given dataType add call has a negative number which is not allowed!");
        }

        if(dataTypeHandlers.containsKey(dataType)) {
            throw new IllegalStateException("Attempted Double Register of datatype for a FormatKodeck! [Byte: " + dataType + "]");
        }

        dataTypeHandlers.put(dataType, kodeck);

        return this;
    }

    public FormatKodeck<T> setAsDataReducible(byte dataType, Function<Object, Collection<Object>> handler){
        if(dataType < 0){
            throw new IllegalStateException("Byte for the given dataType set Reducible call has a negative number which is not allowed!");
        }

        if(dataReducible.containsKey(dataType)) {
            throw new IllegalStateException("Attempted Double Reducible Register of datatype for a FormatKodeck! [Byte: " + dataType + "]");
        }

        dataReducible.put(dataType, handler);

        return this;
    }

    public FormatKodeck<T> dataSaverMode(boolean value) {
        this.dataSaverMode = value;
        return this;
    }

    @Override
    public <E> T decode(Format<E> ops, E object) {
        if (this.dataSaverMode && !dataReducible.isEmpty()) return this.memorySaverDecode(ops, object);

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
        if (this.dataSaverMode && !dataReducible.isEmpty()) return this.memorySaverEncode(ops, object, prefix);

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

    //--

    public <E> T memorySaverDecode(Format<E> ops, E object) {
        T inPrefix = (T)this.format.empty();

        if (ops.getClass().isInstance(this.format)) {
            return (T)object;
        } else if (ops instanceof StructuredFormat<E, ?> outFormat) {
            return outFormat.getFormatKodeck().encode(this.format, object, this.format.empty());
        }

        Byte dataType = this.decodeDataTypeStack.peek();

        boolean saveByte = dataType == -2;

        if (dataType < 0) {
            dataType = ops.getByte(object);
        }

        if (!this.dataTypeHandlers.containsKey(dataType)) {
            throw new IllegalStateException("Unable to process a given dataType due to the format not supporting such! [Byte: " + dataType + "]");
        }

        if (saveByte) {
            this.decodeDataTypeStack.pop();
            this.decodeDataTypeStack.push(dataType);
        }

        Kodeck kodeck = this.dataTypeHandlers.get(dataType);

        this.decodeDataTypeStack.push(Byte.valueOf((byte)(dataReducible.containsKey(dataType) ? -2 : -1)));

        Object decodedValue = kodeck.decode(ops, object);

        this.decodeDataTypeStack.pop();

        return (T)kodeck.encode(this.format, decodedValue, inPrefix);
    }


    public <E, I> E memorySaverEncode(Format<E> ops, T object, E prefix) {
        if (ops.getClass().isInstance(this.format)) return (E)object;

        byte dataType = this.dataTypeGetter.apply(object);

        if (!this.dataTypeHandlers.containsKey(dataType)) {
            throw new IllegalStateException("Unable to process a given dataType due to the format not supporting such!");
        }

        Kodeck<T> kodeck = (Kodeck)this.dataTypeHandlers.get(dataType);

        if (ops instanceof StructuredFormat) {
            return (kodeck == null ? ops.empty() : kodeck.encode(ops, kodeck.decode(this.format, object), prefix));
        }

        MutableBoolean byteLock = this.encodeByteLocks.get(object);

        if (byteLock == null || !byteLock.getValue()) {
            ops.createByte(dataType, prefix);

            if (byteLock != null) byteLock.setValue(true);
        }

        if (kodeck == null) return ops.empty();

        T decodedValue = kodeck.decode(this.format, object);

        var collectionHandler = dataReducible.getOrDefault(dataType, null);

        if (collectionHandler != null) {
            MutableBoolean lock = new MutableBoolean(false);

            for(Object t : collectionHandler.apply(decodedValue)) this.encodeByteLocks.put(t, lock);
        }

        E encodedValue = kodeck.encode(ops, decodedValue, prefix);

        if (collectionHandler != null) {
            for(Object t : collectionHandler.apply(decodedValue)) this.encodeByteLocks.remove(t);
        }

        return encodedValue;
    }

    //--

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

    //--



}
