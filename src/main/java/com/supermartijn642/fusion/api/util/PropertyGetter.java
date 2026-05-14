package com.supermartijn642.fusion.api.util;

import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * Getter for {@link Property}s.
 * <p>
 * Created 12/05/2026 by SuperMartijn642
 */
@FunctionalInterface
public interface PropertyGetter {
    /**
     * Gets an arbitrary property.
     */
    <X, C> Optional<X> getProperty(Property<X,C> property, C context);

    /**
     * Gets an arbitrary property.
     */
    @ApiStatus.NonExtendable
    default <X> Optional<X> getProperty(Property<X,Void> property){
        return this.getProperty(property, null);
    }
}
