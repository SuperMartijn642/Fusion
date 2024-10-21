package com.supermartijn642.fusion;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.entity.model.predicates.*;
import com.supermartijn642.fusion.model.modifiers.item.predicates.*;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import com.supermartijn642.fusion.model.types.connecting.predicates.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
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

    public static final RenderType USE_ORIGINAL_RENDER_TYPE_MARKER = RenderType.create("fusion:ignore", null, null, 0, false, false, RenderType.CompositeState.builder().createCompositeState(false));

    private static NativeImage dummyImage;

    public static NativeImage getDummyImage(){
        if(dummyImage == null){
            dummyImage = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
            dummyImage.setPixelRGBA(0, 0, 0xFFFFFFFF);
        }
        return dummyImage;
    }

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
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_direction"), IsDirectionConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_block_in_front"), MatchBlockInFrontConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_state"), MatchStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(new ResourceLocation("fusion", "match_state_in_front"), MatchStateInFrontConnectionPredicate.SERIALIZER);
        // Register default item model predicates
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "and"), AndItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "or"), OrItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "not"), NotItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "count"), CountItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "durability"), DurabilityItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "enchantment"), EnchantmentItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(new ResourceLocation("fusion", "potion"), PotionItemPredicate.SERIALIZER);
        // Register default entity model predicates
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "and"), AndEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "or"), OrEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "not"), NotEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "altitude"), AltitudeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "is_baby"), BabyEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "biome"), BiomeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(new ResourceLocation("fusion", "dimension"), DimensionEntityModelPredicate.SERIALIZER);

        // Finalize registration
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration()); TODO
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());

        // Integration with FramedBlocks
        FMLJavaModLoadingContext.get().getModEventBus().addListener((Consumer<InterModEnqueueEvent>)event -> InterModComms.sendTo("framedblocks", "add_ct_property", () -> ConnectingBakedModel.BLOCK_CACHE_PROPERTY));
    }

    public static RenderType getRenderTypeMaterial(BaseTextureData.RenderType renderType){
        if(renderType == null)
            return USE_ORIGINAL_RENDER_TYPE_MARKER;
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

    private static String fusionVersion;

    public static String getFusionVersion(){
        if(fusionVersion == null){
            String version = ModList.get().getModContainerById("fusion").orElseThrow().getModInfo().getVersion().toString();
            if(!version.matches("\\d+\\.\\d+\\.\\d+"))
                version = version.substring(0, version.length() - version.replaceFirst("\\d+\\.\\d+\\.\\d+\\D", "").length() - 1);
            fusionVersion = version;
        }
        return fusionVersion;
    }
}
