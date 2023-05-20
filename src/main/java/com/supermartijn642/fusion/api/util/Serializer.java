package com.supermartijn642.fusion.api.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public interface Serializer<T> {

    /**
     * Deserializes the given json to some data.
     * @throws JsonParseException if the given json does not match the expected format
     */
    T deserialize(JsonObject json) throws JsonParseException;

    /**
     * Serializes the given data to json.
     */
    JsonObject serialize(T value);
}
