package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.model.data.VanillaModelDataBuilder;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Pair;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class ConnectingModelDataBuilderImpl implements ConnectingModelDataBuilder {

    private final VanillaModelDataBuilder<?,BlockModel> vanillaModel = VanillaModelDataBuilder.builder();
    private final List<ConnectionPredicate> predicates = new ArrayList<>();

    @Override
    public ConnectingModelDataBuilder connection(ConnectionPredicate predicate){
        this.predicates.add(predicate);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder parent(ResourceLocation parent){
        this.vanillaModel.parent(parent);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, String reference){
        this.vanillaModel.texture(key, reference);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, ResourceLocation texture){
        this.vanillaModel.texture(key, texture);
        return this;
    }

    @Override
    public Pair<BlockModel,List<ConnectionPredicate>> build(){
        return Pair.of(this.vanillaModel.build(), this.predicates);
    }
}
