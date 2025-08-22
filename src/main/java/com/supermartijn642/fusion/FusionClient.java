package com.supermartijn642.fusion;

import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.entity.model.predicates.*;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.model.modifiers.item.predicates.*;
import com.supermartijn642.fusion.model.types.connecting.predicates.*;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.MaterialFinder;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.material.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("fusion");
    private static final RenderMaterial[] RENDER_MATERIALS = new RenderMaterial[(2 | (1 << 2) | ((BaseTextureData.RenderType.values().length) << 3)) + 1];

    public static final ThreadLocal<Boolean> IS_RENDERING_BREAKING_OVERLAY = new ThreadLocal<>();

    @Override
    public void onInitializeClient(){
        // Register default texture types
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "base"), DefaultTextureTypes.BASE);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "scrolling"), DefaultTextureTypes.SCROLLING);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "random"), DefaultTextureTypes.RANDOM);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "continuous"), DefaultTextureTypes.CONTINUOUS);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "vanilla"), DefaultModelTypes.VANILLA);
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "and"), AndConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "or"), OrConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "not"), NotConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_direction"), IsDirectionConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "match_block_in_front"), MatchBlockInFrontConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "match_state"), MatchStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "match_state_in_front"), MatchStateInFrontConnectionPredicate.SERIALIZER);
        // Register default item model predicates
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "and"), AndItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "or"), OrItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "not"), NotItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "count"), CountItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "durability"), DurabilityItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "enchantment"), EnchantmentItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "potion"), PotionItemPredicate.SERIALIZER);
        // Register default entity model predicates
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "and"), AndEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "or"), OrEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "not"), NotEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "altitude"), AltitudeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_baby"), BabyEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "biome"), BiomeEntityModelPredicate.SERIALIZER);
        EntityModelPredicateRegistry.registerEntityModelPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "dimension"), DimensionEntityModelPredicate.SERIALIZER);

        // Add Fusion's metadata section
        SpriteLoader.DEFAULT_METADATA_SECTIONS = ImmutableSet.<MetadataSectionType<?>>builder()
            .addAll(SpriteLoader.DEFAULT_METADATA_SECTIONS)
            .add(FusionTextureMetadataSection.TYPE)
            .build();

        // Finalize registration
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());
    }

    public static RenderMaterial getRenderTypeMaterial(Boolean ambientOcclusion, BaseTextureData.RenderType renderType, boolean emissive){
        int index = (ambientOcclusion == null ? 2 : ambientOcclusion ? 1 : 0)
            | (emissive ? 1 : 0) << 2
            | (renderType == null ? 0 : renderType.ordinal() + 1) << 3;
        RenderMaterial material = RENDER_MATERIALS[index];
        if(material == null){
            MaterialFinder materialFinder = Renderer.get().materialFinder();
            materialFinder.shadeMode(ShadeMode.VANILLA);
            if(ambientOcclusion != null)
                materialFinder.ambientOcclusion(ambientOcclusion ? TriState.TRUE : TriState.FALSE);
            if(renderType != null){
                BlendMode mode = renderType == BaseTextureData.RenderType.OPAQUE ? BlendMode.SOLID
                    : renderType == BaseTextureData.RenderType.CUTOUT ? BlendMode.CUTOUT
                    : renderType == BaseTextureData.RenderType.TRANSLUCENT ? BlendMode.TRANSLUCENT : null;
                materialFinder.blendMode(mode);
            }
            if(emissive)
                materialFinder.emissive(true).disableDiffuse(true).ambientOcclusion(TriState.FALSE);
            material = materialFinder.find();
            RENDER_MATERIALS[index] = material;
        }
        return material;
    }

    private static String fusionVersion;

    public static String getFusionVersion(){
        if(fusionVersion == null){
            String version = FabricLoader.getInstance().getModContainer("fusion").orElseThrow().getMetadata().getVersion().getFriendlyString();
            if(!version.matches("\\d+\\.\\d+\\.\\d+"))
                version = version.substring(0, version.length() - version.replaceFirst("\\d+\\.\\d+\\.\\d+\\D", "").length() - 1);
            fusionVersion = version;
        }
        return fusionVersion;
    }
}
