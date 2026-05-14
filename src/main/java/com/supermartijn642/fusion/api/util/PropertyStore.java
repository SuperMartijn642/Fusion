package com.supermartijn642.fusion.api.util;

import com.supermartijn642.fusion.util.PropertyStoreImpl;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 12/05/2026 by SuperMartijn642
 */
public interface PropertyStore extends PropertyGetter {

    /**
     * Creates a map-backed property store.
     */
    static PropertyStore create(){
        return PropertyStoreImpl.create();
    }

    /**
     * Stores an arbitrary property.
     */
    <X, C> void setProperty(Property<X,C> property, C context, X value);

    /**
     * Stores an arbitrary property.
     */
    @ApiStatus.NonExtendable
    default <X> void setProperty(Property<X,Void> property, X value){
        this.setProperty(property, null, value);
    }

    @ApiStatus.NonExtendable
    default <X, C> X getOrCompute(Property<X,C> property, C context, Function<C,X> compute){
        return this.getProperty(property, context).orElseGet(() -> {
            X value = compute.apply(context);
            this.setProperty(property, context, value);
            return value;
        });
    }

    @ApiStatus.NonExtendable
    default <X> X getOrCompute(Property<X,Void> property, Supplier<X> compute){
        return this.getProperty(property).orElseGet(() -> {
            X value = compute.get();
            this.setProperty(property, value);
            return value;
        });
    }
}
