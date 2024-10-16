package io.wispforest.owo.util;

import io.wispforest.owo.mixin.recipe.RecipeMixin;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 *  Simple Class used to store and register all Recipe Specific Remainders.
 */
public final class RecipeSpecificRemainders {

    private static final Map<Identifier, Set<RecipeRemainder>> allSpecificRemainders = new HashMap<>();

    /**
     *  Method used to register a Recipe specific Item remainder for a given a Recipe
     *
     * @param recipeId The Recipe's Identifier
     * @param item The Item to check for
     * @param itemRemainder The Remainder Item for the given Item
     */
    public static void add(Identifier recipeId, Item item, Item itemRemainder){
        allSpecificRemainders.computeIfAbsent(recipeId, (id) -> new HashSet<>())
                .add(new RecipeRemainder(item, itemRemainder));
    }

    /**
     * Checks if a given Recipe has any {@link RecipeRemainder}'s via its {@link Identifier}
     */
    public static boolean hasRemainders(Identifier recipeId){
        return allSpecificRemainders.containsKey(recipeId);
    }

    /**
     * Returns a {@link Set} full of Recipe Remainders if such Recipe's Identifier exists or empty if not.
     *
     * @param recipeId The Recipe's {@link Identifier}
     * @return The Recipe's Specific Remainders
     */
    public static Set<RecipeRemainder> getRemainders(Identifier recipeId){
        return hasRemainders(recipeId) ? Collections.unmodifiableSet(allSpecificRemainders.get(recipeId)) : Set.of();
    }

    /**
     *  Simple Record used to store an Item, and it's Crafting Remainder (Look at {@link RecipeMixin})
     */
    public final record RecipeRemainder(Item item, Item itemRemainder){}
}
