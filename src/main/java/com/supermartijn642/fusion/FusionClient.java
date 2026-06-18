package com.supermartijn642.fusion;

import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.model.predicates.FusionModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.blockstate.FusionBlockStateModelPredicateRegistry;
import com.supermartijn642.fusion.api.model.predicates.item.FusionItemModelPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.entity.model.predicates.*;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.model.predicates.*;
import com.supermartijn642.fusion.model.predicates.blockstate.*;
import com.supermartijn642.fusion.model.predicates.item.*;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import com.supermartijn642.fusion.texture.types.connecting.predicates.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger(Fusion.MODID);

    public static final ThreadLocal<Object> IS_RENDERING_BREAKING_OVERLAY = new ThreadLocal<>();

    private static NativeImage dummyImage;

    public static NativeImage getDummyImage(){
        if(dummyImage == null){
            dummyImage = new NativeImage(NativeImage.Format.RGBA, 1, 1, false);
            dummyImage.setPixelRGBA(0, 0, 0xFFFFFFFF);
        }
        return dummyImage;
    }

    @Override
    public void onInitializeClient(){
        // Register default texture types
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("base"), DefaultTextureTypes.BASE);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("scrolling"), DefaultTextureTypes.SCROLLING);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("random"), DefaultTextureTypes.RANDOM);
        FusionTextureTypeRegistry.registerTextureType(Fusion.identifier("continuous"), DefaultTextureTypes.CONTINUOUS);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("cuboid"), DefaultModelTypes.CUBOID);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("connecting"), DefaultModelTypes.CONNECTING);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("composite"), DefaultModelTypes.COMPOSITE);
        // Register default connection predicates
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("true"), TrueConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("false"), FalseConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("and"), AndConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("or"), OrConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("not"), NotConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_direction"), IsDirectionConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_block_in_front"), MatchBlockInFrontConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_state"), MatchStateConnectionPredicate.SERIALIZER);
        FusionConnectionPredicateRegistry.registerConnectionPredicate(Fusion.identifier("match_state_in_front"), MatchStateInFrontConnectionPredicate.SERIALIZER);
        // Register default model predicates
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("true"), TrueModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("false"), FalseModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("and"), AndModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("or"), OrModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("not"), NotModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("block_and_item"), BlockAndItemModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("item_wrapper"), ItemWrapperModelPredicate.SERIALIZER);
        FusionModelPredicateRegistry.registerModelPredicate(Fusion.identifier("block_state_wrapper"), BlockStateWrapperModelPredicate.SERIALIZER);
        // Register default block state model predicates
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("true"), TrueBlockStateModelPredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("false"), FalseBlockStateModelPredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("and"), AndBlockStateModelPredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("or"), OrBlockStateModelPredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("altitude"), NotBlockStateModelPredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("biome"), BiomeBlockStatePredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("dimension"), DimensionBlockStateModelPredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("match_block"), MatchBlockBlockStatePredicate.SERIALIZER);
        FusionBlockStateModelPredicateRegistry.registerBlockStateModelPredicate(Fusion.identifier("match_state"), MatchStateBlockStatePredicate.SERIALIZER);
        // Register default item model predicates
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("true"), TrueItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("false"), FalseItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("and"), AndItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("or"), OrItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("not"), NotItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("count"), CountItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("durability"), DurabilityItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("enchantment"), EnchantmentItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("match_custom_name"), MatchCustomNameItemModelPredicate.SERIALIZER);
        FusionItemModelPredicateRegistry.registerItemModelPredicate(Fusion.identifier("potion"), PotionItemModelPredicate.SERIALIZER);
        // Register default entity model predicates
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("true"), TrueEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("false"), FalseEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("and"), AndEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("or"), OrEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("not"), NotEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("altitude"), AltitudeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("is_baby"), BabyEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("biome"), BiomeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistryImpl.registerPredicate(Fusion.identifier("dimension"), DimensionEntityModelPredicate.SERIALIZER);
    }

    public static void finalizeRegistries(){
        TextureTypeRegistryImpl.finalizeRegistration();
        ModelTypeRegistryImpl.finalizeRegistration();
        ConnectionPredicateRegistryImpl.finalizeRegistration();
        ModelPredicateRegistryImpl.finalizeRegistration();
        BlockStateModelPredicateRegistryImpl.finalizeRegistration();
        ItemModelPredicateRegistryImpl.finalizeRegistration();
        EntityModelPredicateRegistryImpl.finalizeRegistration();
    }

    private static String fusionVersion;

    public static String getFusionVersion(){
        if(fusionVersion == null){
            String version = FabricLoader.getInstance().getModContainer(Fusion.MODID).orElseThrow().getMetadata().getVersion().getFriendlyString();
            if(!version.matches("\\d+\\.\\d+\\.\\d+"))
                version = version.substring(0, version.length() - version.replaceFirst("\\d+\\.\\d+\\.\\d+\\D", "").length() - 1);
            fusionVersion = version;
        }
        return fusionVersion;
    }

    public static boolean isRenderingBreakingOverlay(){
        return FusionClient.IS_RENDERING_BREAKING_OVERLAY.get() != null;
    }
}
