package com.supermartijn642.fusion;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import com.supermartijn642.fusion.predicate.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LoggerFactory.getLogger("fusion");

    public static void init(){
        // Register default texture types
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(new ResourceLocation("fusion", "scrolling"), DefaultTextureTypes.SCROLLING);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "vanilla"), DefaultModelTypes.VANILLA);
        FusionModelTypeRegistry.registerModelType(new ResourceLocation("fusion", "connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "and"), AndConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "or"), OrConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "not"), NotConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_block"), MatchBlockConnectionPredicate.SERIALIZER);

        // Finalize registration

//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration()); TODO
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());

        // Integration with FramedBlocks
        FMLJavaModLoadingContext.get().getModEventBus().addListener((Consumer<InterModEnqueueEvent>)event -> InterModComms.sendTo("framedblocks", "add_ct_property", () -> ConnectingBakedModel.SURROUNDING_BLOCK_DATA_MODEL_PROPERTY));
    }

    public static RenderType getRenderTypeMaterial(ConnectingTextureData.RenderType renderType){
        RenderType material;
        //noinspection EnhancedSwitchMigration
        switch(renderType){
            case OPAQUE:
                material = RenderType.solid();
                break;
            case CUTOUT:
                material = RenderType.cutout();
                break;
            case TRANSLUCENT:
                material = RenderType.translucent();
                break;
            default:
                throw new AssertionError();
        }
        return material;
    }
}
