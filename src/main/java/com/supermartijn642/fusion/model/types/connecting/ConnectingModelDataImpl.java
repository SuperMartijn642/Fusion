package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import net.minecraft.client.renderer.block.model.ModelBlock;

import java.util.Map;

/**
 * Created 23/10/2023 by SuperMartijn642
 */
public class ConnectingModelDataImpl implements ConnectingModelData {

    private final ModelBlock model;
    private final Map<String,ConnectionPredicate> predicates;

    public ConnectingModelDataImpl(ModelBlock model, Map<String,ConnectionPredicate> predicates){
        this.model = model;
        this.predicates = ImmutableMap.copyOf(predicates);
    }

    @Override
    public ModelBlock getVanillaModel(){
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
