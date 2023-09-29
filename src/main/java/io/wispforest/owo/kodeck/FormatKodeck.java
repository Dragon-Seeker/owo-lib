package io.wispforest.owo.kodeck;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class FormatKodeck<T> implements Kodeck<T> {

    public Format<T> format;

    public Map<Byte, Kodeck<?>> dataHandlerMap = new HashMap<>();
    public Function<T, Byte> dataTypeGetter;

    public FormatKodeck(KodeckableFormat<T> format, Function<T, Byte> dataTypeGetter){
        this(format, (HandlerRegister<T>) HandlerRegister.DEFAULT, dataTypeGetter);
    }

    public FormatKodeck(Format<T> format, HandlerRegister<T> init, Function<T, Byte> dataTypeGetter){
        this.format = format;
        this.dataTypeGetter = dataTypeGetter;

        init.initData(this);
    }

    @Override
    public <E> T decode(Format<E> ops, E object) {
        var inPrefix = format.empty();

        if(ops.getClass().isInstance(format)){
            return (T) object;
        } else if(ops instanceof KodeckableFormat<E> outFormat){
            return outFormat.getFormatKodeck().encode(format, (E) object, format.empty());
        }

        byte dataType = ops.getByte(object);

        if(!dataHandlerMap.containsKey(dataType)){
            throw new IllegalStateException("Unable to process a given dataType due to the format not supporting such!");
        }

        Kodeck<T> kodeck = (Kodeck<T>) dataHandlerMap.get(dataType);

        if(kodeck == null) return format.empty();

        return kodeck.encode(format, kodeck.decode(ops, object), inPrefix);
    }

    @Override
    public <E> E encode(Format<E> ops, T object, E prefix) {
        if (ops.getClass().isInstance(format)) return (E) object;

        byte dataType = dataTypeGetter.apply(object);

        if(!(ops instanceof KodeckableFormat<E>)) ops.createByte(dataType, prefix);

        if(!dataHandlerMap.containsKey(dataType)){
            throw new IllegalStateException("Unable to process a given dataType due to the format not supporting such!");
        }

        Kodeck<T> kodeck = (Kodeck<T>) dataHandlerMap.get(dataType);

        if(kodeck == null){
            return ops.empty();
        }

        return kodeck.encode(ops, kodeck.decode(format, object), prefix);
    }

    public interface HandlerRegister<T> {
        HandlerRegister<?> DEFAULT = kodeck -> {
            var dataHandlerMap = kodeck.dataHandlerMap;

            dataHandlerMap.put((byte) 0, null);
            dataHandlerMap.put((byte) 1, Kodeck.BOOLEAN);
            dataHandlerMap.put((byte) 2, Kodeck.BYTE);
            dataHandlerMap.put((byte) 3, Kodeck.SHORT);
            dataHandlerMap.put((byte) 4, Kodeck.INT);
            dataHandlerMap.put((byte) 5, Kodeck.LONG);
            dataHandlerMap.put((byte) 6, Kodeck.FLOAT);
            dataHandlerMap.put((byte) 7, Kodeck.DOUBLE);
            dataHandlerMap.put((byte) 8, Kodeck.STRING);
            dataHandlerMap.put((byte) 9, ListKodeck.of(kodeck));
            dataHandlerMap.put((byte) 10, MapKodeck.of(kodeck));
        };

        void initData(FormatKodeck<T> kodeck);
    }
}
