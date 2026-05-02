package com.supermartijn642.fusion.api.model.data;

import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public interface BaseModelData {

    static BaseModelDataBuilder<?,BaseModelData> builder(){
        return BaseModelDataBuilder.builder();
    }

    CuboidModel getVanillaModel();

    List<Identifier> getParents();
}
