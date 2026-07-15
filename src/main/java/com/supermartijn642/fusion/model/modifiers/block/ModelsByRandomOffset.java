package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.model.CombinedBakedModel;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public class ModelsByRandomOffset {

    public static final ThreadLocal<Vector3f> RANDOM_OFFSET_OVERWRITE = new ThreadLocal<>();

    private final List<Entry> entries = new ArrayList<>();
    private int entryIndex = -1;
    private RandomOffsetFunction lastOffsetFunction;

    private BlockPos blockPos;
    private Vec3d defaultBlockOffset;

    public void setContext(BlockPos blockPos, Vec3d defaultBlockOffset){
        this.blockPos = blockPos;
        this.defaultBlockOffset = defaultBlockOffset;
    }

    public void add(RandomOffsetFunction offset, IBakedModel model){
        if(offset.equals(this.lastOffsetFunction)){
            this.entries.get(this.entryIndex).models.add(model);
            return;
        }
        this.entryIndex++;
        if(this.entries.size() <= this.entryIndex)
            this.entries.add(new Entry());
        this.lastOffsetFunction = offset;
        Entry entry = this.entries.get(this.entryIndex);
        offset.getOffset(this.defaultBlockOffset, this.blockPos, entry.offset);
        entry.models.clear();
        entry.models.add(model);
    }

    public List<Entry> getEntries(){
        return this.entries;
    }

    public int getEntryCount(){
        return this.entryIndex + 1;
    }

    public void foreach(Consumer<Entry> consumer){
        for(int i = 0; i <= this.entryIndex; i++)
            consumer.accept(this.entries.get(i));
    }

    public void reset(){
        this.entryIndex = -1;
        this.lastOffsetFunction = null;
    }

    public static class Entry extends CombinedBakedModel {
        final Vector3f offset = new Vector3f();
        final List<IBakedModel> models = new ArrayList<>();

        public Vector3f getOffset(){
            return this.offset;
        }

        @Override
        protected List<IBakedModel> getModels(){
            return this.models;
        }
    }
}
