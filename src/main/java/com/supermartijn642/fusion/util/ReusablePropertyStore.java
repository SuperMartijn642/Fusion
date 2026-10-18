package com.supermartijn642.fusion.util;

import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.api.util.PropertyStore;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Behaves like {@link FallbackPropertyStore}, but can be reset and reused instead of creating a new store (with its
 * maps and entries) for every block during chunk meshing. Resetting only bumps a generation counter, so entries and
 * map nodes are kept and reused. Not thread-safe; intended to be held per thread.
 */
public class ReusablePropertyStore implements PropertyStore {

    // Clear everything once this many slots exist, so keys from previous resource reloads can't accumulate forever
    private static final int MAX_SLOTS = 4096;

    private final Map<Property<?,?>,Entry> properties = new HashMap<>();
    private final Map<Object,Object> scratch = new HashMap<>(4);
    private PropertyGetter fallback;
    private int generation = 1;
    private int slotCount;

    public void reset(PropertyGetter fallback){
        this.fallback = fallback;
        if(++this.generation == 0 || this.slotCount > MAX_SLOTS){
            this.properties.clear();
            this.slotCount = 0;
            this.generation = 1;
        }
    }

    /**
     * Returns an object that lives as long as this store, so per-use helper objects can be reset instead of
     * reallocated. The caller must fully reinitialize it before each use.
     */
    public <T> T scratch(Object key, Supplier<T> factory){
        Object value = this.scratch.get(key);
        if(value == null)
            this.scratch.put(key, value = factory.get());
        //noinspection unchecked
        return (T)value;
    }

    // Entries, maps and slots are kept across resets, but each records the generation in which a fresh
    // PropertyStoreImpl would have had it, so lookups (including its failure cases) behave exactly like a new store

    @Override
    public <X, C> void setProperty(Property<X,C> property, C context, X value){
        Entry entry = this.properties.get(property);
        if(entry == null || entry.generation != this.generation){
            if(value == null)
                return;
            if(entry == null)
                this.properties.put(property, entry = new Entry());
            entry.generation = this.generation;
        }
        entry.setValue(context, value);
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context){
        Entry entry = this.properties.get(property);
        Object value = entry == null || entry.generation != this.generation ? null : entry.getValue(context);
        if(value != null)
            //noinspection unchecked
            return Optional.of((X)value);
        return this.fallback.getProperty(property, context);
    }

    private final class Entry {
        private int generation;
        private final Slot defaultValue = new Slot();
        private Map<Object,Slot> mappedValues;
        private int mappedValuesGeneration;

        private void setValue(Object key, Object value){
            int generation = ReusablePropertyStore.this.generation;
            if(key == null){
                this.defaultValue.value = value;
                this.defaultValue.generation = generation;
                return;
            }
            boolean hasMappedValues = this.mappedValuesGeneration == generation;
            if(value == null){
                if(hasMappedValues){
                    Slot slot = this.mappedValues.get(key);
                    if(slot != null)
                        slot.generation = 0;
                }
                return;
            }
            if(this.mappedValues == null)
                this.mappedValues = new HashMap<>();
            this.mappedValuesGeneration = generation;
            Slot slot = this.mappedValues.get(key);
            if(slot == null){
                this.mappedValues.put(key, slot = new Slot());
                ReusablePropertyStore.this.slotCount++;
            }
            slot.value = value;
            slot.generation = generation;
        }

        private Object getValue(Object key){
            int generation = ReusablePropertyStore.this.generation;
            if(key == null)
                return this.defaultValue.generation == generation ? this.defaultValue.value : null;
            // PropertyStoreImpl throws here too, as its mapped values don't exist yet
            if(this.mappedValuesGeneration != generation)
                throw new NullPointerException("No mapped values for keyed property lookup");
            Slot slot = this.mappedValues.get(key);
            return slot == null || slot.generation != generation ? null : slot.value;
        }
    }

    private static final class Slot {
        private Object value;
        private int generation;
    }
}
