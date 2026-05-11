package com.supermartijn642.fusion.util;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Created 24/05/2026 by SuperMartijn642
 */
public class ChunkRenderTypeMap<T> implements Map<RenderType,T> {

    private static final int RENDER_TYPES;
    private static final Map<RenderType,Integer> RENDER_TYPE_TO_ID;
    private static final RenderType[] ID_TO_RENDER_TYPE;

    static{
        List<RenderType> renderTypes = ChunkRenderTypeHelper.allChunkRenderTypes();
        RENDER_TYPES = renderTypes.size() + 1;
        ImmutableMap.Builder<RenderType,Integer> renderTypeToId = ImmutableMap.builderWithExpectedSize(renderTypes.size());
        ID_TO_RENDER_TYPE = new RenderType[renderTypes.size() + 1];
        for(int i = 0; i < renderTypes.size(); i++){
            RenderType renderType = renderTypes.get(i);
            renderTypeToId.put(renderType, i + 1);
            ID_TO_RENDER_TYPE[i + 1] = renderType;
        }
        RENDER_TYPE_TO_ID = renderTypeToId.build();
        if(RENDER_TYPES > 32)
            throw new AssertionError("More than 31 chunk render types!");
    }

    private static int getId(@Nullable RenderType renderType){
        if(renderType == null)
            return 0;
        Integer id = RENDER_TYPE_TO_ID.get(renderType);
        if(id == null)
            throw new IllegalArgumentException("Key must be a chunk render type!");
        return id;
    }

    @Nullable
    private static RenderType byId(int id){
        return ID_TO_RENDER_TYPE[id];
    }

    private static boolean isValidKey(Object key){
        return key == null || (key instanceof RenderType && RENDER_TYPE_TO_ID.containsKey(key));
    }

    private int keys;
    @SuppressWarnings("unchecked")
    private final T[] values = (T[])new Object[RENDER_TYPES];
    private int size;

    private Set<RenderType> keySet;
    private Collection<T> valuesCollection;
    private Set<Entry<RenderType,T>> entrySet;

    @Override
    public int size(){
        return this.size;
    }

    @Override
    public boolean isEmpty(){
        return this.size == 0;
    }

    @Override
    public boolean containsKey(Object key){
        if(!isValidKey(key))
            return false;
        int id = getId((RenderType)key);
        return (this.keys & (1 << id)) != 0;
    }

    @Override
    public boolean containsValue(Object value){
        if(value == null){
            for(T v : this.values){
                if(v == null)
                    return true;
            }
        }else{
            for(T v : this.values){
                if(value.equals(v))
                    return true;
            }
        }
        return false;
    }

    private boolean containsMapping(Object key, Object value){
        if(!isValidKey(key))
            return false;
        int id = getId((RenderType)key);
        return (this.keys & (1 << id)) != 0 && Objects.equals(this.values[id], value);
    }

    @Override
    public T get(Object key){
        if(!isValidKey(key))
            return null;
        int id = getId((RenderType)key);
        return this.values[id];
    }

    @Override
    public @Nullable T put(RenderType key, T value){
        if(!isValidKey(key))
            throw new IllegalArgumentException("Key must be a chunk render type!");
        int id = getId(key);
        T old = this.values[id];
        this.values[id] = value;
        if((this.keys & (1 << id)) == 0){
            this.size++;
            this.keys |= (1 << id);
        }
        return old;
    }

    @Override
    public T remove(Object key){
        if(!isValidKey(key))
            return null;
        int id = getId((RenderType)key);
        if((this.keys & (1L << id)) == 0)
            return null;
        this.size--;
        this.keys &= ~(1 << id);
        T old = this.values[id];
        this.values[id] = null;
        return old;
    }

    @Override
    public void putAll(@NotNull Map<? extends RenderType,? extends T> m){
        m.forEach(this::put);
    }

    @Override
    public void clear(){
        this.size = 0;
        this.keys = 0;
        Arrays.fill(this.values, null);
    }

    @Override
    public @NotNull Set<RenderType> keySet(){
        if(this.keySet == null)
            this.keySet = new KeySet();
        return this.keySet;
    }

    @Override
    public @NotNull Collection<T> values(){
        if(this.valuesCollection == null)
            this.valuesCollection = new ValuesCollection();
        return this.valuesCollection;
    }

    @Override
    public @NotNull Set<Entry<RenderType,T>> entrySet(){
        if(this.entrySet == null)
            this.entrySet = new EntrySet();
        return this.entrySet;
    }

    private class KeySet extends AbstractSet<RenderType> {
        @Override
        public Iterator<RenderType> iterator(){
            return new KeyIterator();
        }

        @Override
        public int size(){
            return ChunkRenderTypeMap.this.size;
        }

        @Override
        public boolean contains(Object o){
            return ChunkRenderTypeMap.this.containsKey(o);
        }

        @Override
        public boolean remove(Object o){
            int oldSize = ChunkRenderTypeMap.this.size;
            ChunkRenderTypeMap.this.remove(o);
            return ChunkRenderTypeMap.this.size != oldSize;
        }

        @Override
        public void clear(){
            ChunkRenderTypeMap.this.clear();
        }
    }

    private class KeyIterator extends AbstractIterator<RenderType> {
        @Override
        public RenderType next(){
            if(!this.hasNext())
                throw new NoSuchElementException();
            return ID_TO_RENDER_TYPE[this.index++];
        }
    }

    private class ValuesCollection extends AbstractCollection<T> {
        @Override
        public Iterator<T> iterator(){
            return new ValueIterator();
        }

        @Override
        public int size(){
            return ChunkRenderTypeMap.this.size;
        }

        @Override
        public boolean contains(Object o){
            return ChunkRenderTypeMap.this.containsValue(o);
        }

        @Override
        public boolean remove(Object o){
            for(int i = 0; i < ChunkRenderTypeMap.this.values.length; i++){
                if((ChunkRenderTypeMap.this.keys & (1 << i)) != 0 && Objects.equals(o, ChunkRenderTypeMap.this.values[i])){
                    ChunkRenderTypeMap.this.size--;
                    ChunkRenderTypeMap.this.keys &= ~(1 << i);
                    ChunkRenderTypeMap.this.values[i] = null;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear(){
            ChunkRenderTypeMap.this.clear();
        }
    }

    private class ValueIterator extends AbstractIterator<T> {
        @Override
        public T next(){
            if(!this.hasNext())
                throw new NoSuchElementException();
            return ChunkRenderTypeMap.this.values[this.index++];
        }
    }

    private class EntrySet extends AbstractSet<Entry<RenderType,T>> {
        @Override
        public Iterator<Entry<RenderType,T>> iterator(){
            return new EntryIterator();
        }

        @Override
        public boolean contains(Object o){
            return o instanceof Map.Entry<?,?> e && ChunkRenderTypeMap.this.containsMapping(e.getKey(), e.getValue());
        }

        @Override
        public boolean remove(Object o){
            if(!this.contains(o))
                return false;
            ChunkRenderTypeMap.this.remove(((Entry<?,?>)o).getKey());
            return true;
        }

        @Override
        public int size(){
            return ChunkRenderTypeMap.this.size;
        }

        @Override
        public void clear(){
            ChunkRenderTypeMap.this.clear();
        }

        @Override
        public Object[] toArray(){
            return this.fillEntryArray(new Object[ChunkRenderTypeMap.this.size]);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <V> V[] toArray(V[] a){
            int size = this.size();
            if(a.length < size)
                a = (V[])Array.newInstance(a.getClass().getComponentType(), size);
            if(a.length > size)
                a[size] = null;
            return (V[])this.fillEntryArray(a);
        }

        private Object[] fillEntryArray(Object[] a){
            int j = 0;
            for(int i = 0; i < ChunkRenderTypeMap.this.values.length; i++){
                if((ChunkRenderTypeMap.this.keys & (1 << i)) != 0)
                    a[j++] = new AbstractMap.SimpleEntry<>(byId(i), ChunkRenderTypeMap.this.values[i]);
            }
            return a;
        }
    }

    private class EntryIterator extends AbstractIterator<Entry<RenderType,T>> {
        private Entry lastEntry = null;

        @Override
        public Map.Entry<RenderType,T> next(){
            if(!this.hasNext())
                throw new NoSuchElementException();
            return this.lastEntry = new Entry(this.index++);
        }

        @Override
        public void remove(){
            super.remove();
            if(this.lastEntry != null)
                this.lastEntry.index = -1;
        }

        private class Entry implements Map.Entry<RenderType,T> {
            private int index;

            private Entry(int index){
                this.index = index;
            }

            public RenderType getKey(){
                this.checkIndex();
                return byId(this.index);
            }

            public T getValue(){
                this.checkIndex();
                return ChunkRenderTypeMap.this.values[this.index];
            }

            public T setValue(T value){
                this.checkIndex();
                T old = ChunkRenderTypeMap.this.values[this.index];
                ChunkRenderTypeMap.this.values[this.index] = value;
                return old;
            }

            public boolean equals(Object o){
                if(this.index < 0)
                    return o == this;

                if(!(o instanceof Map.Entry<?,?> e))
                    return false;

                return Objects.equals(e.getKey(), byId(this.index)) && Objects.equals(e.getValue(), ChunkRenderTypeMap.this.values[this.index]);
            }

            public int hashCode(){
                if(this.index < 0)
                    return super.hashCode();

                return byId(this.index).hashCode() ^ ChunkRenderTypeMap.this.values[this.index].hashCode();
            }

            public String toString(){
                if(this.index < 0)
                    return super.toString();

                return byId(this.index) + "=" + ChunkRenderTypeMap.this.values[this.index];
            }

            private void checkIndex(){
                if(this.index < 0)
                    throw new IllegalStateException("Entry was removed!");
            }
        }
    }

    private abstract class AbstractIterator<V> implements Iterator<V> {
        int index = 0, lastIndex = -1;

        @Override
        public boolean hasNext(){
            while(this.index < ChunkRenderTypeMap.this.values.length && (ChunkRenderTypeMap.this.keys & (1 << this.index)) == 0)
                this.index++;
            return this.index < ChunkRenderTypeMap.this.values.length;
        }

        @Override
        public void remove(){
            if(this.lastIndex < 0)
                throw new IllegalStateException();
            ChunkRenderTypeMap.this.remove(byId(this.lastIndex));
            this.lastIndex = -1;
        }
    }
}
