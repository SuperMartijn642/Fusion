package com.supermartijn642.fusion;

import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.entity.model.predicates.*;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.model.predicates.item.*;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import com.supermartijn642.fusion.texture.types.connecting.predicates.*;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LoggerFactory.getLogger(Fusion.MODID);

    public static final ThreadLocal<Boolean> IS_RENDERING_BREAKING_OVERLAY = new ThreadLocal<>();

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
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("cuboid"), DefaultModelTypes.CUBOID);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(Fusion.identifier("connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
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
        // Register default item model predicates
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("and"), AndItemModelPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("or"), OrItemModelPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("not"), NotItemModelPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("count"), CountItemModelPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("durability"), DurabilityItemModelPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("enchantment"), EnchantmentItemModelPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("potion"), PotionItemModelPredicate.SERIALIZER);
        // Register default entity model predicates
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("and"), AndEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("or"), OrEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("not"), NotEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("altitude"), AltitudeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("is_baby"), BabyEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("biome"), BiomeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Fusion.identifier("dimension"), DimensionEntityModelPredicate.SERIALIZER);

        // Add Fusion's metadata section
        SpriteLoader.DEFAULT_METADATA_SECTIONS = ImmutableSet.<MetadataSectionSerializer<?>>builder()
            .addAll(SpriteLoader.DEFAULT_METADATA_SECTIONS)
            .add(FusionTextureMetadataSection.INSTANCE)
            .build();

        // Integration with FramedBlocks TODO
//        ModLoadingContext.get().getActiveContainer().getEventBus().addListener((Consumer<InterModEnqueueEvent>)event -> {
//            InterModComms.sendTo("framedblocks", "add_ct_property", () -> ConnectingBlockStateModel.PREDICATES_EVALUATION_PROPERTY);
//        });
    }

    public static void finalizeRegistries(){
        TextureTypeRegistryImpl.finalizeRegistration();
        ModelTypeRegistryImpl.finalizeRegistration();
        ConnectionPredicateRegistryImpl.finalizeRegistration();
        EntityModelPredicateRegistry.finalizeRegistration();
        ItemPredicateRegistry.finalizeRegistration();
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
