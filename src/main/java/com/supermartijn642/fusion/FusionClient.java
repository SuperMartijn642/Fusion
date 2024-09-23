package com.supermartijn642.fusion;

import com.google.common.collect.ImmutableSet;
import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.predicate.FusionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.item.predicates.*;
import com.supermartijn642.fusion.model.types.connecting.ConnectingBakedModel;
import com.supermartijn642.fusion.predicate.*;
import com.supermartijn642.fusion.texture.FusionTextureMetadataSection;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.SpriteLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LoggerFactory.getLogger("fusion");

    public static final RenderType USE_ORIGINAL_RENDER_TYPE_MARKER = RenderType.create("fusion:ignore", null, null, 0, RenderType.CompositeState.builder().createCompositeState(false));

    public static void init(){
        // Register default texture types
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "vanilla"), DefaultTextureTypes.VANILLA);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "base"), DefaultTextureTypes.BASE);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "connecting"), DefaultTextureTypes.CONNECTING);
        FusionTextureTypeRegistry.registerTextureType(ResourceLocation.fromNamespaceAndPath("fusion", "scrolling"), DefaultTextureTypes.SCROLLING);
        // Register default model types
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "unknown"), DefaultModelTypes.UNKNOWN);
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "vanilla"), DefaultModelTypes.VANILLA);
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "base"), DefaultModelTypes.BASE);
        FusionModelTypeRegistry.registerModelType(ResourceLocation.fromNamespaceAndPath("fusion", "connecting"), DefaultModelTypes.CONNECTING);
        // Register default connection predicates
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "and"), AndConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "or"), OrConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "not"), NotConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_face_visible"), IsFaceVisibleConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_same_block"), IsSameBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "is_same_state"), IsSameStateConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "match_block"), MatchBlockConnectionPredicate.SERIALIZER);
        FusionPredicateRegistry.registerConnectionPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "match_state"), MatchStateConnectionPredicate.SERIALIZER);
        // Register default item model predicates
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "and"), AndItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "or"), OrItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "not"), NotItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "count"), CountItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "durability"), DurabilityItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "enchantment"), EnchantmentItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(ResourceLocation.fromNamespaceAndPath("fusion", "potion"), PotionItemPredicate.SERIALIZER);

        // Add Fusion's metadata section
        SpriteLoader.DEFAULT_METADATA_SECTIONS = ImmutableSet.<MetadataSectionSerializer<?>>builder()
            .addAll(SpriteLoader.DEFAULT_METADATA_SECTIONS)
            .add(FusionTextureMetadataSection.INSTANCE)
            .build();

        // Finalize registration

//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> TextureTypeRegistryImpl.finalizeRegistration()); TODO
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> ModelTypeRegistryImpl.finalizeRegistration());
//        ClientLifecycleEvents.CLIENT_STARTED.register(client -> PredicateRegistryImpl.finalizeRegistration());

        // Integration with FramedBlocks
        ModLoadingContext.get().getActiveContainer().getEventBus().addListener((Consumer<InterModEnqueueEvent>)event -> InterModComms.sendTo("framedblocks", "add_ct_property", () -> ConnectingBakedModel.BLOCK_CACHE_PROPERTY));
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
}
