package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.client.renderer.model.BlockModel;

import java.util.Map;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl implements ConnectingModelData {

    private final BlockModel model;
    private final Map<String,ConnectionPredicate> predicates;

    public ConnectingModelDataImpl(BlockModel model, Map<String,ConnectionPredicate> predicates){
        this.model = model;
        this.predicates = ImmutableMap.copyOf(predicates);
    }

    @Override
    public BlockModel getVanillaModel(){
        return this.model;
    }

    @Override
    public ConnectionPredicate getConnectionPredicate(String texture){
        return this.predicates.get(texture);
    }

    @Override
    public ConnectionPredicate getDefaultConnectionPredicate(){
        return this.getConnectionPredicate("default");
    }

    @Override
    public Map<String,ConnectionPredicate> getAllConnectionPredicates(){
        return this.predicates;
    }
}
