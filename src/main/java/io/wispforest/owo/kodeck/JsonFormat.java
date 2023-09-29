package io.wispforest.owo.kodeck;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.nbt.NbtElement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JsonFormat implements Format<JsonElement> {

    public static JsonFormat INSTANCE = new JsonFormat();

    public static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final TypeToken<Map<String, JsonElement>> STRING_MAP_TYPE_TOKEN = new TypeToken<>(){};

    private static final TypeToken<List<JsonElement>> LIST_TYPE_TOKEN = new TypeToken<>(){};

//    public <U> U convertTo(Format<U> outFormat, JsonElement input) {
//        if (outFormat instanceof JsonFormat) {
//            return (U)input;
//        } else if (input instanceof JsonObject jsonObject) {
//            Map<String, U> map = (Map)this.getStringBasedMap(jsonObject)
//                    .entrySet()
//                    .stream()
//                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> this.convertTo(outFormat, (JsonElement)entry.getValue())));
//            return (U)outFormat.createStringBasedMap(map);
//        } else if (input instanceof JsonArray jsonArray) {
//            List<U> list = this.getList(jsonArray).stream().map(element -> this.convertTo(outFormat, element)).toList();
//            return (U)outFormat.createList(list);
//        } else if (input instanceof JsonPrimitive primitive) {
//            if (primitive.isString()) {
//                return (U)outFormat.createString(primitive.getAsString());
//            } else if (primitive.isBoolean()) {
//                return (U)outFormat.createBoolean(primitive.getAsBoolean());
//            } else {
//                BigDecimal value = primitive.getAsBigDecimal();
//
//                try {
//                    long l = value.longValueExact();
//                    if ((long)((byte)((int)l)) == l) {
//                        return (U)outFormat.createByte((byte)((int)l));
//                    } else if ((long)((short)((int)l)) == l) {
//                        return (U)outFormat.createShort((short)((int)l));
//                    } else {
//                        return (U)((long)((int)l) == l ? outFormat.createInt((int)l) : outFormat.createLong(l));
//                    }
//                } catch (ArithmeticException var10) {
//                    double d = value.doubleValue();
//                    return (U)((double)((float)d) == d ? outFormat.createFloat((float)d) : outFormat.createDouble(d));
//                }
//            }
//        } else {
//            throw new IllegalStateException("Unknown JsonElement Object: " + input);
//        }
//    }

    //--

    @Override
    public JsonElement empty() {
        return JsonNull.INSTANCE;
    }

    @Override
    public JsonElement createBoolean(boolean value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createByte(byte value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createShort(short value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createInt(int value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createLong(long value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createFloat(float value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createDouble(double value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createString(String value, JsonElement prefix) {
        return GSON.toJsonTree(value);
    }

    @Override
    public JsonElement createStringBasedMap(int size, JsonElement prefix) {
        return new JsonObject();
    }

    @Override
    public JsonElement addMapEntry(String key, Supplier<JsonElement> input, JsonElement prefix) {
        ((JsonObject) prefix).add(key, input.get());

        return prefix;
    }

    @Override
    public JsonElement createList(int size, JsonElement prefix) {
        return new JsonArray();
    }

    @Override
    public JsonElement addListEntry(JsonElement input, JsonElement prefix) {
        ((JsonArray) prefix).add(input);

        return prefix;
    }

    //--


    @Override
    public boolean getBoolean(JsonElement input) {
        return GSON.fromJson(input, Boolean.class);
    }

    @Override
    public byte getByte(JsonElement input) {
        return GSON.fromJson(input, Byte.class);
    }

    @Override
    public short getShort(JsonElement input) {
        return GSON.fromJson(input, Short.class);
    }

    @Override
    public int getInt(JsonElement input) {
        return GSON.fromJson(input, Integer.class);
    }

    @Override
    public long getLong(JsonElement input) {
        return GSON.fromJson(input, Long.class);
    }

    @Override
    public float getFloat(JsonElement input) {
        return GSON.fromJson(input, Float.class);
    }

    @Override
    public double getDouble(JsonElement input) {
        return GSON.fromJson(input, Double.class);
    }

    @Override
    public String getString(JsonElement input) {
        return GSON.fromJson(input, String.class);
    }

    @Override
    public Stream<Map.Entry<String, JsonElement>> getStringBasedMap(JsonElement input) {
        return GSON.fromJson(input, STRING_MAP_TYPE_TOKEN).entrySet().stream();
    }

    @Override
    public Stream<JsonElement> getList(JsonElement input) {
        return GSON.fromJson(input, LIST_TYPE_TOKEN).stream();
    }
}
