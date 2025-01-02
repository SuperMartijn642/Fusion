package com.supermartijn642.fusion.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.*;

import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created 27/12/2024 by SuperMartijn642
 */
public class CodecHelper {

    public static <T> Codec<T> jsonSerializerToCodec(Function<T,JsonElement> serializer, Function<JsonElement,T> deserializer) {
        return new Codec<>() {
            @Override
            public <U> DataResult<U> encode(T input, DynamicOps<U> ops, U prefix){
                // Serialize the input to json
                JsonElement json;
                try{
                    json = serializer.apply(input);
                }catch(Exception e){
                    return DataResult.error(() -> "Failed to serialize object: " + e.getMessage());
                }
                // Convert json to the required format
                return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json));
            }

            @Override
            public <U> DataResult<Pair<T,U>> decode(DynamicOps<U> ops, U input){
                // Convert the input to json
                JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
                // Deserialize from json
                T object;
                try{
                    object = deserializer.apply(json);
                }catch(Exception e){
                    return DataResult.error(() -> "Failed to deserialize object: " + e.getMessage());
                }
                return DataResult.success(Pair.of(object,input));
            }
        };
    }

    public static <T> MapCodec<T> jsonSerializerToMapCodec(Function<T,JsonObject> serializer, Function<JsonObject,T> deserializer) {
        return new MapCodec<>() {
            @Override
            public <T1> Stream<T1> keys(DynamicOps<T1> ops){
                return Stream.empty();
            }

            @Override
            public <T1> RecordBuilder<T1> encode(T input, DynamicOps<T1> ops, RecordBuilder<T1> prefix){
                // Serialize the input to json
                JsonObject json = serializer.apply(input);
                // Convert all entries to the required format and add them the record builder
                for(String key : json.keySet())
                    prefix.add(key, JsonOps.INSTANCE.convertTo(ops, json.get(key)));
                return prefix;
            }

            @Override
            public <T1> DataResult<T> decode(DynamicOps<T1> ops, MapLike<T1> input){
                // Convert the input to json
                JsonObject json = new JsonObject();
                input.entries()
                    .map(entry -> {
                        DataResult<String> key = ops.getStringValue(entry.getFirst());
                        return key.isSuccess() ? Pair.of(key.getOrThrow(), entry.getSecond()) : null;
                    })
                    .filter(Objects::nonNull)
                    .forEach(entry -> json.add(entry.getFirst(), ops.convertTo(JsonOps.INSTANCE, entry.getSecond())));
                // Deserialize from json
                T object;
                try{
                    object = deserializer.apply(json);
                }catch(Exception e){
                    return DataResult.error(() -> "Failed to deserialize object: " + e.getMessage());
                }
                return DataResult.success(object);
            }
        };
    }
}
