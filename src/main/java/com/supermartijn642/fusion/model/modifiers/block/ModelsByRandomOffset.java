package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.model.CombinedBakedModel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public class ModelsByRandomOffset {

    public static final ThreadLocal<Vector3fc> RANDOM_OFFSET_OVERWRITE = new ThreadLocal<>();

    private final List<Entry> entries = new ArrayList<>();
    private int entryIndex = -1;
    private RandomOffsetFunction lastOffsetFunction;

    private BlockPos blockPos;
    private Vec3 defaultBlockOffset;

    public void setContext(BlockPos blockPos, Vec3 defaultBlockOffset){
        this.blockPos = blockPos;
        this.defaultBlockOffset = defaultBlockOffset;
    }

    public void add(RandomOffsetFunction offset, BakedModel model, @Nullable ModelData modelData){
        if(offset.equals(this.lastOffsetFunction)){
            Entry entry = this.entries.get(this.entryIndex);
            entry.models.add(model);
            entry.modelData.add(modelData);
            return;
        }
        this.entryIndex++;
        if(this.entries.size() <= this.entryIndex)
            this.entries.add(new Entry());
        this.lastOffsetFunction = offset;
        Entry entry = this.entries.get(this.entryIndex);
        offset.getOffset(this.defaultBlockOffset, this.blockPos, entry.offset);
        entry.models.clear();
        entry.modelData.clear();
        entry.models.add(model);
        entry.modelData.add(modelData);
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
        final List<BakedModel> models = new ArrayList<>();
        final List<ModelData> modelData = new ArrayList<>();

        public Vector3fc getOffset(){
            return this.offset;
        }

        @Override
        protected List<BakedModel> getModels(){
            return this.models;
        }

        @Override
        protected ModelData getModelData(int modelIndex, BakedModel model, BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData){
            ModelData subData = this.modelData.get(modelIndex);
            return subData == null ? super.getModelData(modelIndex, model, level, pos, state, modelData) : subData;
        }
    }
}
