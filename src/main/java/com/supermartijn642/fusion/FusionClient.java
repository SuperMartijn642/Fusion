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
import com.supermartijn642.fusion.model.types.connecting.predicates.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(Fusion.MODID);

    public static final RenderType USE_ORIGINAL_RENDER_TYPE_MARKER = RenderType.create("fusion:ignore", null, null, 0, false, false, RenderType.CompositeState.builder().createCompositeState(false));

    public static final ThreadLocal<Boolean> IS_RENDERING_BREAKING_OVERLAY = new ThreadLocal<>();

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
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("base"), DefaultTextureTypes.BASE);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("scrolling"), DefaultTextureTypes.SCROLLING);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("random"), DefaultTextureTypes.RANDOM);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("continuous"), DefaultTextureTypes.CONTINUOUS);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("vanilla"), DefaultModelTypes.VANILLA);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("and"), AndConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("or"), OrConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("not"), NotConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_direction"), IsDirectionConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_block_in_front"), MatchBlockInFrontConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_state"), MatchStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_state_in_front"), MatchStateInFrontConnectionPredicate.SERIALIZER);
        // Register default item model predicates
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("and"), AndItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("or"), OrItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("not"), NotItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("count"), CountItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("durability"), DurabilityItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("enchantment"), EnchantmentItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("potion"), PotionItemPredicate.SERIALIZER);
        // Register default entity model predicates
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("and"), AndEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("or"), OrEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("not"), NotEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("altitude"), AltitudeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("is_baby"), BabyEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("biome"), BiomeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("dimension"), DimensionEntityModelPredicate.SERIALIZER);

        // Finalize registration
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration()); TODO
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());
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
            String version = ModList.get().getModContainerById(Fusion.MODID).orElseThrow().getModInfo().getVersion().toString();
            if(!version.matches("\\d+\\.\\d+\\.\\d+"))
                version = version.substring(0, version.length() - version.replaceFirst("\\d+\\.\\d+\\.\\d+\\D", "").length() - 1);
            fusionVersion = version;
        }
        return fusionVersion;
    }
}
