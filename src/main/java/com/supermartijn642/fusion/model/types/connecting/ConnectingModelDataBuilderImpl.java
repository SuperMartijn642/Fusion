package com.supermartijn642.fusion.model.types.connecting;

import com.google.common.collect.ImmutableMap;
import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import net.minecraft.util.ResourceLocation;

import java.util.*;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class ConnectingModelDataBuilderImpl implements ConnectingModelDataBuilder {

    private final BaseModelDataBuilder<?,BaseModelData> baseModel = BaseModelData.builder();
    private final Map<String,List<ConnectionPredicate>> predicates = new HashMap<>();

    @Override
    public ConnectingModelDataBuilder parent(ResourceLocation parent){
        this.baseModel.parent(parent);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder parents(ResourceLocation... parents){
        this.baseModel.parents(parents);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, String reference){
        this.baseModel.texture(key, reference);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, ResourceLocation texture){
        this.baseModel.texture(key, texture);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder connection(ConnectionPredicate predicate){
        return this.connection("default", predicate);
    }

    @Override
    public ConnectingModelDataBuilder connection(String texture, ConnectionPredicate predicate){
        this.predicates.computeIfAbsent("default", s -> new ArrayList<>()).add(predicate);
        return this;
    }

    @Override
    public ConnectingModelData build(){
        BaseModelDataImpl baseData = (BaseModelDataImpl)this.baseModel.build();
        ImmutableMap.Builder<String,ConnectionPredicate> predicates = ImmutableMap.builder();
        this.predicates.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), DefaultConnectionPredicates.or(entry.getValue().toArray(new ConnectionPredicate[0]))))
            .forEach(pair -> predicates.put(pair.left(), pair.right()));
        //noinspection rawtypes,unchecked
        return new ConnectingModelDataImpl(baseData.getVanillaModel(), baseData.getParents(), (List)baseData.getElements(), predicates.build(), Collections.emptyMap());
    }
}
