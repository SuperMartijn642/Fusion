package com.supermartijn642.fusion.api.util;

import com.supermartijn642.fusion.util.PropertyStoreImpl;
import org.jetbrains.annotations.ApiStatus;

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
}
