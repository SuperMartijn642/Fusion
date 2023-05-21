package com.supermartijn642.fusion.mixin;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.model.FusionBlockModel;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.predicate.PredicateRegistryImpl;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Type;

/**
 * Created 31/03/2023 by SuperMartijn642
 */
@Mixin(value = ModelLoaderRegistry.ExpandedBlockModelDeserializer.class, priority = 900)
public class BlockModelDeserializerMixin {

    @Unique
    private static boolean shouldIgnore = false;

    @Inject(
        method = "deserialize(Lcom/google/gson/JsonElement;Ljava/lang/reflect/Type;Lcom/google/gson/JsonDeserializationContext;)Lnet/minecraft/client/renderer/block/model/BlockModel;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void deserialize(JsonElement json, Type type, JsonDeserializationContext context, CallbackInfoReturnable<BlockModel> ci) throws JsonParseException{
        if(shouldIgnore)
            return;

        JsonElement loaderJson = json.getAsJsonObject().get("loader");
        if(loaderJson != null && loaderJson.isJsonPrimitive() && loaderJson.getAsJsonPrimitive().isString() && IdentifierUtil.isValidIdentifier(loaderJson.getAsString())){
            ResourceLocation loader = new ResourceLocation(loaderJson.getAsString());
            if(loader.getNamespace().equals("fusion") && loader.getPath().equals("model")){
                // Finalize model type registration
                ModelTypeRegistryImpl.finalizeRegistration();
                // Finalize predicate registration
                PredicateRegistryImpl.finalizeRegistration();

                // Load the model data
                shouldIgnore = true;
                ModelInstance<?> model = ModelTypeRegistryImpl.deserializeModelData(json.getAsJsonObject());
                shouldIgnore = false;

                // Create a dummy block model
                FusionBlockModel newModel = new FusionBlockModel(model);
                ci.setReturnValue(newModel);
            }
        }
    }
}
