package io.wispforest.owo.serialization;

import io.wispforest.endec.SerializationAttribute;
import io.wispforest.owo.mixin.CachedRegistryInfoGetterAccessor;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryOps;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class RegistriesAttribute implements SerializationAttribute.Instance {

    public static final SerializationAttribute.WithValue<RegistriesAttribute> REGISTRIES = SerializationAttribute.withValue("registries");

    private final RegistryOps.RegistryInfoGetter infoGetter;
    private final @Nullable DynamicRegistryManager registryManager;

    private RegistriesAttribute(RegistryOps.RegistryInfoGetter infoGetter, @Nullable DynamicRegistryManager registryManager) {
        this.infoGetter = infoGetter;
        this.registryManager = registryManager;
    }

    public static RegistriesAttribute of(DynamicRegistryManager registryManager) {
        return new RegistriesAttribute(
                new RegistryOps.CachedRegistryInfoGetter(registryManager),
                registryManager
        );
    }

    @ApiStatus.Internal
    public static RegistriesAttribute tryFromCachedInfoGetter(RegistryOps.RegistryInfoGetter lookup) {
        return (lookup instanceof RegistryOps.CachedRegistryInfoGetter cachedGetter)
                ? fromCachedInfoGetter(cachedGetter)
                : fromInfoGetter(lookup);
    }

    public static RegistriesAttribute fromCachedInfoGetter(RegistryOps.CachedRegistryInfoGetter cachedGetter) {
        DynamicRegistryManager registryManager = null;

        if(((CachedRegistryInfoGetterAccessor) (Object) cachedGetter).owo$getRegistriesLookup() instanceof DynamicRegistryManager drm) {
            registryManager = drm;
        }

        return new RegistriesAttribute(cachedGetter, registryManager);
    }

    public static RegistriesAttribute fromInfoGetter(RegistryOps.RegistryInfoGetter lookup) {
        return new RegistriesAttribute(lookup, null);
    }

    public RegistryOps.RegistryInfoGetter infoGetter() {
        return this.infoGetter;
    }

    public boolean hasRegistryManager() {
        return this.registryManager != null;
    }

    public @NotNull DynamicRegistryManager registryManager() {
        if (!this.hasRegistryManager()) {
            throw new IllegalStateException("This instance of RegistriesAttribute does not supply a DynamicRegistryManager");
        }

        return this.registryManager;
    }

    @Override
    public SerializationAttribute attribute() {
        return REGISTRIES;
    }

    @Override
    public Object value() {
        return this;
    }
}
