package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.BlockBreakingStateExtension;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.state.level.BlockBreakingRenderState;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

/**
 * Created 14/07/2026 by SuperMartijn642
 */
@Mixin(BlockBreakingRenderState.class)
public class BlockBreakingStateMixin implements BlockBreakingStateExtension {

    @Unique
    private List<Triple<Vector3fc,List<BlockStateModelPart>,Boolean>> fusionParts;

    @Override
    public List<Triple<Vector3fc,List<BlockStateModelPart>,Boolean>> getFusionParts(){
        return this.fusionParts;
    }

    @Override
    public void setFusionParts(List<Triple<Vector3fc,List<BlockStateModelPart>,Boolean>> parts){
        this.fusionParts = parts;
    }
}
