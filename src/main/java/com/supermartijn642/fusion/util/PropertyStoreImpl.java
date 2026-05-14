package com.supermartijn642.fusion.util;

import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyStore;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created 12/05/2026 by SuperMartijn642
 */
public class PropertyStoreImpl implements PropertyStore {

    public static PropertyStore create(){
        return new PropertyStoreImpl();
    }

    private final Map<Property<?,?>,Entry> properties = new HashMap<>(4, 1);

    private PropertyStoreImpl(){
    }

    @Override
    public <X, C> void setProperty(Property<X,C> property, C context, X value){
        Entry entry = value == null ?
            this.properties.get(property) :
            this.properties.computeIfAbsent(property, Entry::new);
        if(entry == null)
            return;
        entry.setValue(context, value);
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        Entry entry = this.properties.get(property);
        if(entry == null)
            return Optional.empty();
        //noinspection unchecked
        return Optional.ofNullable((X)entry.getValue(context));
    }

    private static class Entry {
        private final Property<?,?> property;
        private Object defaultValue;
        private Map<Object,Object> mappedValues;

        private Entry(Property<?,?> property){
            this.property = property;
        }

        private void setValue(Object key, Object value){
            if(key == null){
                this.defaultValue = value;
                return;
            }
            if(value == null){
                if(this.mappedValues != null)
                    this.mappedValues.remove(key);
            }else{
                if(this.mappedValues == null){
                    //noinspection rawtypes,unchecked
                    this.mappedValues = this.property.contextType().isEnum() ?
                        new EnumMap(this.property.contextType()) :
                        new HashMap<>();
                }
                this.mappedValues.put(key, value);
            }
        }

        private Object getValue(Object key){
            if(key == null)
                return this.defaultValue;
            return this.mappedValues.get(key);
        }
    }
}
