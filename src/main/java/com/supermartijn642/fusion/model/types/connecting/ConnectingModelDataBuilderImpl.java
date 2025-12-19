package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import net.minecraft.resources.Identifier;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class ConnectingModelDataBuilderImpl implements ConnectingModelDataBuilder {

    private final BaseModelDataBuilder<?,BaseModelData> baseModel = BaseModelData.builder();
    private final Map<String,List<ConnectionPredicate>> predicates = new HashMap<>();

    @Override
    public ConnectingModelDataBuilder parent(Identifier parent){
        this.baseModel.parent(parent);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder parents(Identifier... parents){
        this.baseModel.parents(parents);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, String reference){
        this.baseModel.texture(key, reference);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder texture(String key, Identifier texture){
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
        Map<String,ConnectionPredicate> predicates = this.predicates.entrySet().stream()
            .map(entry -> Pair.of(entry.getKey(), DefaultConnectionPredicates.or(entry.getValue().toArray(ConnectionPredicate[]::new))))
            .collect(Collectors.toUnmodifiableMap(Pair::left, Pair::right));
        //noinspection rawtypes,unchecked
        return new ConnectingModelDataImpl(baseData.getVanillaModel(), baseData.getParents(), (List)baseData.getElements(), predicates, Collections.emptyMap());
    }
}
