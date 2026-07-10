package com.supermartijn642.fusion.model.modifiers.block;

import com.supermartijn642.fusion.model.CombinedBlockStateModel;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

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
    private Vec3 defaultBlockOffset;

    public void setContext(BlockPos blockPos, Vec3 defaultBlockOffset){
        this.blockPos = blockPos;
        this.defaultBlockOffset = defaultBlockOffset;
    }

    public void add(RandomOffsetFunction offset, BlockStateModel model){
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

    public static class Entry extends CombinedBlockStateModel {
        final Vector3f offset = new Vector3f();
        final List<BlockStateModel> models = new ArrayList<>();

        public Vector3fc getOffset(){
            return this.offset;
        }

        @Override
        protected List<BlockStateModel> getModels(){
            return this.models;
        }
    }
}
