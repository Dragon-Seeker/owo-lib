package io.wispforest.owo.itemgroup;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record ItemGroupReference(Supplier<@NotNull OwoItemGroup> group, int tab) {

    public ItemGroupReference(OwoItemGroup group, int tab) {
        this(() -> group, tab);
    }
}
