package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.joml.Vector3fc;

import java.util.List;

/**
 * Created 14/07/2026 by SuperMartijn642
 */
public interface BlockBreakingStateExtension {

    List<Triple<Vector3fc,List<BlockStateModelPart>,Boolean>> getFusionParts();

    void setFusionParts(List<Triple<Vector3fc,List<BlockStateModelPart>,Boolean>> parts);
}
