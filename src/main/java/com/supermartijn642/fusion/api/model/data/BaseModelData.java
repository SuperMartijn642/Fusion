package com.supermartijn642.fusion.api.model.data;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public interface BaseModelData {

    static BaseModelDataBuilder<?,BaseModelData> builder(){
        return BaseModelDataBuilder.builder();
    }

    BlockModel getVanillaModel();

    List<Identifier> getParents();
}
