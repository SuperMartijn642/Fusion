package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.client.renderer.block.model.BlockModel;

import java.util.Map;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public interface ConnectingModelData {

    static ConnectingModelDataBuilder builder(){
        return ConnectingModelDataBuilder.builder();
    }

    BlockModel getVanillaModel();

    ConnectionPredicate getConnectionPredicate(String texture);

    ConnectionPredicate getDefaultConnectionPredicate();

    Map<String,ConnectionPredicate> getAllConnectionPredicates();
}
