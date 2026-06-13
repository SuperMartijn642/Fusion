package com.supermartijn642.fusion.api.model.types.connecting;

import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelDataBuilderImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Data for the connection model type. This is an extension of the base model data.
 * <p>
 * Created 23/10/2023 by SuperMartijn642
 * @see com.supermartijn642.fusion.api.model.DefaultModelTypes#CONNECTING
 * @see BaseModelData
 */
@ApiStatus.NonExtendable
public interface ConnectingModelData extends BaseModelData {

    /**
     * The fallback key used when a connections key cannot be resolved.
     */
    String DEFAULT_KEY = "default";

    /**
     * Creates a builder for connecting model data.
     */
    static ConnectingModelDataBuilder<?,ConnectingModelData> builder(){
        return ConnectingModelDataBuilderImpl.builder();
    }

    /**
     * Gets the predicate for the given connections key.
     * The result is {@code null} if the given key is absent or if the given key's value is a reference.
     */
    @Nullable
    ConnectionPredicate getConnectionPredicate(String key);

    /**
     * Gets the predicate for the default connections key.
     * The result is {@code null} if the default key is absent or if the default key's value is a reference.
     * @see #DEFAULT_KEY
     */
    @Nullable
    default ConnectionPredicate getDefaultConnectionPredicate(){
        return this.getConnectionPredicate(DEFAULT_KEY);
    }

    /**
     * Gets all connections references.
     * Each key points to either a connection predicate or to another connections key.
     */
    Map<String,Either<String,ConnectionPredicate>> getAllConnectionPredicates();

    @ApiStatus.NonExtendable
    interface ConnectingModelDataBuilder<T extends Builder<T,S>, S> extends Builder<T,S> {

        /**
         * Sets the default connection predicate for this model.
         * @see #DEFAULT_KEY
         * @see DefaultConnectionPredicates
         */
        default T defaultConnections(ConnectionPredicate predicate){
            return this.connections(DEFAULT_KEY, predicate);
        }

        /**
         * Sets the connections for the given key to the given value.
         * @see DefaultConnectionPredicates
         */
        T connections(String key, Either<String,ConnectionPredicate> predicate);

        /**
         * Sets the connection predicate for the given key.
         * @see DefaultConnectionPredicates
         */
        default T connections(String key, ConnectionPredicate predicate){
            return this.connections(key, Either.right(predicate));
        }

        /**
         * Sets the given connections key to redirect to the given reference.
         * @see DefaultConnectionPredicates
         */
        default T connections(String key, String reference){
            return this.connections(key, Either.left(reference));
        }
    }
}
