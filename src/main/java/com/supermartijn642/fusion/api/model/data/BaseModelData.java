package com.supermartijn642.fusion.api.model.data;

import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;

import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public interface BaseModelData {

    static BaseModelDataBuilder<?,BaseModelData> builder(){
        return BaseModelDataBuilder.builder();
    }

    ModelBlock getVanillaModel();

    List<ResourceLocation> getParents();
}
