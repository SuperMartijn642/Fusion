package com.supermartijn642.fusion;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.entity.model.predicates.*;
import com.supermartijn642.fusion.model.FusionModelLoader;
import com.supermartijn642.fusion.model.modifiers.item.predicates.*;
import com.supermartijn642.fusion.model.types.connecting.predicates.*;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Unit;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LoggerFactory.getLogger("fusion");

    public static final ThreadLocal<Unit> IS_RENDERING_BREAKING_OVERLAY = new ThreadLocal<>();

    public static void init(){
        // Register default texture types
        FusionTextureTypeRegistry.registerTextureType(Identifier.fromNamespaceAndPath("fusion", "vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(Identifier.fromNamespaceAndPath("fusion", "base"), DefaultTextureTypes.BASE);
        FusionTextureTypeRegistry.registerTextureType(Identifier.fromNamespaceAndPath("fusion", "connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(Identifier.fromNamespaceAndPath("fusion", "scrolling"), DefaultTextureTypes.SCROLLING);
        FusionTextureTypeRegistry.registerTextureType(Identifier.fromNamespaceAndPath("fusion", "random"), DefaultTextureTypes.RANDOM);
        FusionTextureTypeRegistry.registerTextureType(Identifier.fromNamespaceAndPath("fusion", "continuous"), DefaultTextureTypes.CONTINUOUS);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(Identifier.fromNamespaceAndPath("fusion", "unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(Identifier.fromNamespaceAndPath("fusion", "vanilla"), DefaultModelTypes.VANILLA);
        FusionModelTypeRegistry.registerModelType(Identifier.fromNamespaceAndPath("fusion", "base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(Identifier.fromNamespaceAndPath("fusion", "connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "and"), AndConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "or"), OrConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "not"), NotConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "is_direction"), IsDirectionConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "match_block_in_front"), MatchBlockInFrontConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "match_state"), MatchStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(Identifier.fromNamespaceAndPath("fusion", "match_state_in_front"), MatchStateInFrontConnectionPredicate.SERIALIZER);
        // Register default item model predicates
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "and"), AndItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "or"), OrItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "not"), NotItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "count"), CountItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "durability"), DurabilityItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "enchantment"), EnchantmentItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Identifier.fromNamespaceAndPath("fusion", "potion"), PotionItemPredicate.SERIALIZER);
        // Register default entity model predicates
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "and"), AndEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "or"), OrEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "not"), NotEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "altitude"), AltitudeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "is_baby"), BabyEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "biome"), BiomeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(Identifier.fromNamespaceAndPath("fusion", "dimension"), DimensionEntityModelPredicate.SERIALIZER);

        // Register Fusion model loader
        ModLoadingContext.get().getActiveContainer().getEventBus().addListener(
            (Consumer<ModelEvent.RegisterLoaders>)e -> e.register(IdentifierUtil.withFusionNamespace("model"), new FusionModelLoader())
        );

        // Finalize registration

//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration()); TODO
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());
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
