package com.supermartijn642.fusion.api.model.data;

import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelDataBuilderImpl;
import net.minecraft.client.renderer.block.model.BlockModel;

import java.util.List;

/**
 * Created 01/05/2023 by SuperMartijn642
 */
public interface ConnectingModelDataBuilder extends VanillaModelDataBuilder<ConnectingModelDataBuilder,Pair<BlockModel,List<ConnectionPredicate>>> {

    static ConnectingModelDataBuilder builder(){
        return new ConnectingModelDataBuilderImpl();
    }

    /**
     * Adds a new connection predicate. Of the added predicates, only one needs to be satisfied to form a connection.
     * In case multiple predicates should be satisfied, use {@link DefaultConnectionPredicates#and(ConnectionPredicate...)}.
     */
    ConnectingModelDataBuilder connection(ConnectionPredicate predicate);
}
