package com.supermartijn642.fusion.model.types.connecting;

import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.BaseModelDataBuilder;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.base.BaseModelDataImpl;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created 02/05/2023 by SuperMartijn642
 */
public class ConnectingModelDataBuilderImpl implements ConnectingModelDataBuilder {

    private final BaseModelDataBuilder<?,BaseModelData> baseModel = BaseModelData.builder();
    private final Map<String,Either<ConnectionPredicate,String>> connections = new LinkedHashMap<>(); // This should maintain order

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
    public ConnectingModelDataBuilder texture(String key, Material material){
        this.baseModel.texture(key, material);
        return this;
    }

    @Override
    public ConnectingModelDataBuilder defaultConnections(ConnectionPredicate predicate){
        return this.connections("default", predicate);
    }

    @Override
    public ConnectingModelDataBuilder connections(String key, ConnectionPredicate predicate){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Connections key must only contain characters [a-zA-Z_]!");
        if(this.connections.containsKey(key))
            throw new RuntimeException("Duplicate connections for key '" + key + "'!");

        this.connections.put(key, Either.left(predicate));
        return this;
    }

    @Override
    public ConnectingModelDataBuilder connections(String key, String reference){
        if(!key.matches("[a-zA-Z_]*"))
            throw new IllegalArgumentException("Connections key must only contain characters [a-zA-Z_]!");
        if(this.connections.containsKey(key))
            throw new RuntimeException("Duplicate connections entry for key '" + key + "'!");

        // Remove leading '#'
        if(reference.charAt(0) == '#')
            reference = reference.substring(1);

        this.connections.put(key, Either.right(reference));
        return this;
    }

    @Override
    public ConnectingModelData build(){
        BaseModelDataImpl baseData = (BaseModelDataImpl)this.baseModel.build();
        //noinspection rawtypes,unchecked
        return new ConnectingModelDataImpl(baseData.getVanillaModel(), baseData.getParents(), (List)baseData.getElements(), this.connections);
    }
}
