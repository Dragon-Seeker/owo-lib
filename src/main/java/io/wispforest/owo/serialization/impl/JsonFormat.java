package io.wispforest.owo.serialization.impl;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import io.wispforest.owo.serialization.impl.kodecks.FormatKodeck;
import io.wispforest.owo.serialization.Kodeck;
import io.wispforest.owo.serialization.StructuredFormat;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public final class JsonFormat implements StructuredFormat<JsonElement, JsonObject> {

    public static JsonFormat INSTANCE = new JsonFormat();

    public static final Gson GSON = new GsonBuilder().setLenient().create();

    private static final TypeToken<Map<String, JsonElement>> STRING_MAP_TYPE_TOKEN = new TypeToken<>(){};

    private static final TypeToken<List<JsonElement>> LIST_TYPE_TOKEN = new TypeToken<>(){};

    @Override
    public FormatKodeck<JsonElement> getFormatKodeck() {
        return (FormatKodeck<JsonElement>) Kodeck.JSON_ELEMENT;
    }

    @Override
    public JsonElement get(JsonObject map, String key) {
        return map.get(key);
    }

    @Override
    public JsonElement put(JsonObject map, String key, JsonElement value) {
        var oldValue = map.get(key);

        map.add(key, value);

        return oldValue;
    }

    @Override
    public JsonElement delete(JsonObject map, String key) {
        return map.remove(key);
    }

    @Override
    public boolean contains(JsonObject map, String key) {
        return map.has(key);
    }

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
        if(prefix instanceof JsonObject prefixObject) return prefixObject;

        return new JsonObject();
    }

    @Override
    public JsonElement addMapEntry(String key, Supplier<JsonElement> input, JsonElement prefix) {
        ((JsonObject) prefix).add(key, input.get());

        return prefix;
    }

    @Override
    public JsonElement createList(int size, JsonElement prefix) {
        if(prefix instanceof JsonArray prefixArray) return prefixArray;

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
