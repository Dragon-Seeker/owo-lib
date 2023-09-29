package io.wispforest.owo.nbt;

import io.wispforest.owo.kodeck.Kodeck;
import io.wispforest.owo.kodeck.NbtFormat;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;

/**
 * A utility class for serializing data into {@link NbtCompound}
 * instances. {@link Type} instances are used for holding the
 * actual serializer functions while the key itself carries information
 * about what string key to use.
 * <p>
 * In order to conveniently use instances of this class, employ the methods
 * defined on {@link NbtCarrier} - this is interface-injected onto
 * {@link ItemStack} and {@link NbtCompound} by default
 *
 * @param <T> The type of data a given instance can serialize
 */
public class NbtKeyKodeck<T> {

    protected final String key;
    protected final Kodeck<T> kodeck;

    /**
     * Creates a new key instance used for storing data of type
     * {@code T} into NBT compounds with the given string as key
     *
     * @param key  The string key to use as index into the NBT compound
     * @param type The type object that holds the serializer implementations
     */
    public NbtKeyKodeck(String key, Kodeck<T> kodeck) {
        this.key = key;
        this.kodeck = kodeck;
    }

    /**
     * @deprecated Use {@link NbtCarrier#get(NbtKeyKodeck)} instead
     */
    @Deprecated
    public T get(@NotNull NbtCompound nbt) {
        return kodeck.decode(NbtFormat.SAFE, nbt.get(this.key));
    }

    /**
     * @deprecated Use {@link NbtCarrier#put(NbtKeyKodeck, T)} instead
     */
    @Deprecated
    public void put(@NotNull NbtCompound nbt, T value) {
        nbt.put(this.key,  kodeck.encode(NbtFormat.SAFE, value));
    }

    /**
     * @deprecated Use {@link NbtCarrier#delete(NbtKeyKodeck)} instead
     */
    @Deprecated
    public void delete(@NotNull NbtCompound nbt) {
        nbt.remove(this.key);
    }

    /**
     * @deprecated Use {@link NbtCarrier#has(NbtKeyKodeck)} instead
     */
    @Deprecated
    public boolean isIn(@NotNull NbtCompound nbt) {
        return nbt.contains(this.key);
    }
}