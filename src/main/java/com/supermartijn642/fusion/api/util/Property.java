package com.supermartijn642.fusion.api.util;

/**
 * A key for some typed property.
 * <p>
 * Created 01/05/2026 by SuperMartijn642
 * @param <X> value type of the property
 * @param <C> context type of the property
 * @see com.supermartijn642.fusion.api.model.ModelInstance#getProperty(Property, Object)
 */
public final class Property<X, C> {

    /**
     * Creates a new property key.
     * @param <X> value type of the property
     * @param <C> context type of the property
     */
    public static <X, C> Property<X,C> create(Class<C> contextType){
        return new Property<>(contextType);
    }

    /**
     * Creates a new property key.
     * @param <X> value type of the property
     */
    public static <X> Property<X,Void> create(){
        return new Property<>(Void.class);
    }

    /**
     * Type of the property context.
     */
    public Class<C> contextType(){
        return this.contextType;
    }

    private final Class<C> contextType;

    private Property(Class<C> contextType){
        this.contextType = contextType;
    }
}
