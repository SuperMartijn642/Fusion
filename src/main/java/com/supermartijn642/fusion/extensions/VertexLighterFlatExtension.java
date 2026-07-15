package com.supermartijn642.fusion.extensions;

import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import net.minecraft.client.renderer.Vector3f;
import net.minecraftforge.client.model.pipeline.BlockInfo;

/**
 * Created 14/09/2024 by SuperMartijn642
 */
public interface VertexLighterFlatExtension {

    void setFusionCustomTinting(BaseTextureData.QuadTinting tinting);

    void setFusionRandomOffsetOverwrite(Vector3f offset);

    Vector3f getFusionRandomOffsetOverwrite();

    BlockInfo fusionGetBlockInfo();
}
