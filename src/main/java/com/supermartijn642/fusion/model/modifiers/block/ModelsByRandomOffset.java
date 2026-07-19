package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.model.CombinedBakedModel;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.ILightReader;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created 08/07/2026 by SuperMartijn642
 */
public class ModelsByRandomOffset {

    private final List<Entry> entries = new ArrayList<>();
    private int entryIndex = -1;
    private RandomOffsetFunction lastOffsetFunction;

    private BlockPos blockPos;
    private Vec3d defaultBlockOffset;

    public void setContext(BlockPos blockPos, Vec3d defaultBlockOffset){
        this.blockPos = blockPos;
        this.defaultBlockOffset = defaultBlockOffset;
    }

    public void add(RandomOffsetFunction offset, IBakedModel model, @Nullable IModelData modelData){
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
        final List<IBakedModel> models = new ArrayList<>();
        final List<IModelData> modelData = new ArrayList<>();

        public Vector3f getOffset(){
            return this.offset;
        }

        @Override
        protected List<IBakedModel> getModels(){
            return this.models;
        }

        @Override
        protected IModelData getModelData(int modelIndex, IBakedModel model, ILightReader level, BlockPos pos, BlockState state, IModelData modelData){
            IModelData subData = this.modelData.get(modelIndex);
            return subData == null ? super.getModelData(modelIndex, model, level, pos, state, modelData) : subData;
        }
    }
}
