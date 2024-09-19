package com.supermartijn642.fusion;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.model.FusionBlockModel;
import com.supermartijn642.fusion.model.overlays.BlockModelOverlayReloadListener;
import com.supermartijn642.fusion.predicate.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.IUnbakedModel;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.resources.SimpleReloadableResourceManager;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LogManager.getLogger("fusion");

    public static void init(){
        // Register default texture types
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "base"), DefaultTextureTypes.BASE);
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "scrolling"), DefaultTextureTypes.SCROLLING);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "vanilla"), DefaultModelTypes.VANILLA);
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "and"), AndConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "or"), OrConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "not"), NotConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_state"), MatchStateConnectionPredicate.SERIALIZER);

        // Finalize registration
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration()); TODO
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());

        // Register block model overlay reload listener
        FMLJavaModLoadingContext.get().getModEventBus().addListener((Consumer<ParticleFactoryRegisterEvent>)event -> {
            // Forge's Mixin version doesn't allow for proper constructor mixins, so rather than registering our reload listener
            // before the model manager in the Minecraft constructor, use the resource manager internals to add it at a specific index
            SimpleReloadableResourceManager resourceManager = (SimpleReloadableResourceManager)Minecraft.getInstance().getResourceManager();
            ModelManager modelManager = Minecraft.getInstance().getModelManager();
            int index = resourceManager.listeners.indexOf(modelManager);
            if(index == -1){
                for(int i = 0; i < resourceManager.listeners.size(); i++){
                    if(resourceManager.listeners.get(i) instanceof ModelManager){
                        index = i;
                        break;
                    }
                }
            }
            if(index == -1)
                throw new RuntimeException("Fusion could not find model manager in resource manager reload listeners!");
            resourceManager.listeners.add(index, BlockModelOverlayReloadListener.INSTANCE);
        });
    }

    public static Function<ResourceLocation,IUnbakedModel> getProperModel(Function<ResourceLocation,IUnbakedModel> modelGetter){
        return location -> {
            IUnbakedModel model = modelGetter.apply(location);
            if(model instanceof FusionBlockModel)
                return ((FusionBlockModel)model).hasVanillaModel() ? ((FusionBlockModel)model).getVanillaModel() : FusionBlockModel.getDummyModel();
            return model;
        };
    }

    public static BlockRenderLayer getRenderTypeMaterial(BaseTextureData.RenderType renderType){
        if(renderType == null)
            return null;
        BlockRenderLayer material;
        switch(renderType){
            case OPAQUE:
                material = BlockRenderLayer.SOLID;
                break;
            case CUTOUT:
                material = BlockRenderLayer.CUTOUT;
                break;
            case TRANSLUCENT:
                material = BlockRenderLayer.TRANSLUCENT;
                break;
            default:
                throw new AssertionError();
        }
        return material;
    }
}
