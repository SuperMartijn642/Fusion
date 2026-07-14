package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import org.joml.Vector3fc;

import java.util.List;

/**
 * Created 14/07/2026 by SuperMartijn642
 */
public interface BlockBreakingStateExtension {

    List<Pair<Vector3fc,List<BlockStateModelPart>>> getFusionParts();

    void setFusionParts(List<Pair<Vector3fc,List<BlockStateModelPart>>> parts);
}
