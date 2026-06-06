package com.supermartijn642.fusion.util;

import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.api.util.PropertyStore;

import java.util.Optional;

/**
 * Created 13/05/2026 by SuperMartijn642
 */
public class FallbackPropertyStore implements PropertyStore {

    public static PropertyStore create(PropertyGetter fallback){
        return new FallbackPropertyStore(null, fallback);
    }

    public static PropertyStore create(PropertyStore propertyStore, PropertyGetter fallback){
        return new FallbackPropertyStore(propertyStore, fallback);
    }

    private final PropertyGetter fallback;
    private PropertyStore store;

    private FallbackPropertyStore(PropertyStore propertyStore, PropertyGetter fallback){
        this.store = propertyStore;
        this.fallback = fallback;
    }

    @Override
    public <X, C> void setProperty(Property<X,C> property, C context, X value){
        if(this.store == null)
            this.store = PropertyStore.create();
        this.store.setProperty(property, context, value);
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        if(this.store == null)
            return this.fallback.getProperty(property, context);
        Optional<X> value = this.store.getProperty(property, context);
        return value.isPresent() ? value : this.fallback.getProperty(property, context);
    }
}
