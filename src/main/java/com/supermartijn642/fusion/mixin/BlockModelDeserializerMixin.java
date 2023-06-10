package com.supermartijn642.fusion.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModel;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.predicate.PredicateRegistryImpl;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

/**
 * Created 31/03/2023 by SuperMartijn642
 */
@Mixin(value = ModelBlock.Deserializer.class, priority = 900)
public class BlockModelDeserializerMixin {

    @Unique
    private static final ThreadLocal<Boolean> SHOULD_IGNORE = ThreadLocal.withInitial(() -> false);

    @Inject(
        method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/ModelBlock;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void deserialize(JsonElement json, Type type, JsonDeserializationContext context, CallbackInfoReturnable<ModelBlock> ci) throws JsonParseException{
        if(SHOULD_IGNORE.get())
            return;

        ModelTypeRegistryImpl.finalizeRegistration();
        PredicateRegistryImpl.finalizeRegistration();
        JsonElement loaderJson = json.getAsJsonObject().get("loader");
        if(loaderJson != null && loaderJson.isJsonPrimitive() && loaderJson.getAsJsonPrimitive().isString() && IdentifierUtil.isValidIdentifier(loaderJson.getAsString())){
            ResourceLocation loader = new ResourceLocation(loaderJson.getAsString());
            if(loader.getResourceDomain().equals("fusion") && loader.getResourcePath().equals("model")){
                // Finalize model type registration
                ModelTypeRegistryImpl.finalizeRegistration();
                // Finalize predicate registration
                PredicateRegistryImpl.finalizeRegistration();

                // Load the model data
                SHOULD_IGNORE.set(true);
                ModelInstance<?> model = ModelTypeRegistryImpl.deserializeModelData(json.getAsJsonObject());
                SHOULD_IGNORE.set(false);

                // Create a dummy block model
                FusionBlockModel newModel = new FusionBlockModel(model);
                ci.setReturnValue(newModel);
            }
        }
    }
}
