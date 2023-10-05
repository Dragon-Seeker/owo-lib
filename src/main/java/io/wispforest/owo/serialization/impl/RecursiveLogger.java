package io.wispforest.owo.serialization.impl;

import io.wispforest.owo.serialization.impl.PacketBufFormat;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

@ApiStatus.Internal
public class RecursiveLogger {

    protected boolean logData = false;

    protected Map<String, Object> loggedData = new LinkedHashMap<>();

    protected Deque<DataAccessHelper<?>> dataAccessStack = new ArrayDeque<>();

    protected final void log(String keyType, Object object){
        if(!this.logData) return;

        dataAccessStack.peek().addData(keyType, object);
    }

    public final void startLogger(){
        this.logData = true;

        this.resetLogger();
    }

    public final void resetLogger(){
        loggedData.clear();

        dataAccessStack.clear();
        dataAccessStack.push(new DataAccessHelper<>("base", loggedData));
    }

    public final Map<String, Object> getLoggedData(){
        this.logData = false;

        return loggedData;
    }

    @Nullable
    public <T> DataAccessHelper<T> makeDataAccessHelper(String key, Supplier<T> data){
        if(!logData) return null;

        var helper = new DataAccessHelper<>(key, data.get());

        this.dataAccessStack.push(helper);

        return helper;
    }

    public class DataAccessHelper<T> {
        public String key;
        public T data;

        public DataAccessHelper(String key, T data){
            this.data = data;
            this.key = key;
        }

        public void addData(String keyType, Object object){
            if(data instanceof Map<?, ?> map){
                int index = 1;

                String objectKey = keyType + "_" + index;

                while(map.containsKey(objectKey)){
                    index++;

                    objectKey = keyType + "_" + index;
                }

                ((Map<String, Object>) map).put(objectKey, object);
            } else if(data instanceof List<?> list) {
                ((List<Object>) list).add(new BetterPair<>(keyType, object));
            }
        }

        public void pop(){
            dataAccessStack.pop();
            dataAccessStack.peek().addData(key, data);
        }
    }

    public static class BetterPair<A, B> extends Pair<A, B> {
        public BetterPair(A left, B right) {
            super(left, right);
        }

        @Override
        public String toString() {
            return getLeft().toString() + ": " + getRight().toString();
        }
    }
}
