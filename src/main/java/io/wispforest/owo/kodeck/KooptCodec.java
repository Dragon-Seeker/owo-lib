package io.wispforest.owo.kodeck;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.dynamic.ForwardingDynamicOps;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class KooptCodec<T> implements Codec<T>, Kodeck<T> {

    public static Map<DynamicOps<?>, Format<?>> MAP = new HashMap<>();

    static {
        MAP.put(JsonOps.INSTANCE, JsonFormat.INSTANCE);
        MAP.put(NbtOps.INSTANCE, NbtFormat.SAFE);
    }

    private final Kodeck<T> kodeck;

    public KooptCodec(Kodeck<T> kodeck){
        this.kodeck = kodeck;
    }

    @Nullable
    private static <T1> Format<T1> convertToFormat(DynamicOps<T1> ops){
        if(ops instanceof ForwardingDynamicOps<T1> forwardDynOps) ops = forwardDynOps.delegate;

        return (Format<T1>) MAP.get(ops);
    }

    @Override
    public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
        Format<T1> format = convertToFormat(ops);

        boolean error = true;
        Exception exception = null;

        T value = null;

        if(format != null) {
            try {
                value = decode(format, input);

                error = false;
            } catch (Exception e) {
                exception = e;
            }
        }

        return error
                ? DataResult.error(exception != null ? exception::getMessage : () -> "The corresponding Format for the given DynamicOps could not be located!")
                : DataResult.success(new Pair<>(value, input));
    }

    @Override
    public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
        Format<T1> format = convertToFormat(ops);

        boolean error = true;
        Exception exception = null;

        T1 value = null;

        if(format != null) {
            try {
                value = encode(format, input, prefix);

                error = false;
            } catch (Exception e) {
                exception = e;
            }
        }

        return error
                ? DataResult.error(exception != null ? exception::getMessage : () -> "The corresponding Format for the given DynamicOps could not be located!")
                : DataResult.success(value);
    }

    //--

    @Override
    public <E> T decode(Format<E> ops, E object) {
        return kodeck.decode(ops, object);
    }

    @Override
    public <E> E encode(Format<E> ops, T object, E prefix) {
        return kodeck.encode(ops, object, prefix);
    }
}
