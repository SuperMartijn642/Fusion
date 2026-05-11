package com.supermartijn642.fusion;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.FusionModelTypeRegistry;
import com.supermartijn642.fusion.api.model.types.connecting.predicates.FusionConnectionPredicateRegistry;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.FusionTextureTypeRegistry;
import com.supermartijn642.fusion.api.texture.data.BaseTextureData;
import com.supermartijn642.fusion.model.BlockRenderContext;
import com.supermartijn642.fusion.model.ModelTypeRegistryImpl;
import com.supermartijn642.fusion.model.modifiers.item.predicates.*;
import com.supermartijn642.fusion.model.types.connecting.predicates.*;
import com.supermartijn642.fusion.texture.TextureTypeRegistryImpl;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Created 26/04/2023 by SuperMartijn642
 */
public class FusionClient {

    public static final Logger LOGGER = LogManager.getLogger(Fusion.MODID);

    public static final ThreadLocal<Boolean> IS_RENDERING_BREAKING_OVERLAY = new ThreadLocal<>();
    public static final ThreadLocal<BlockRenderContext> BLOCK_RENDER_CONTEXT = new ThreadLocal<>();
    public static final ThreadLocal<ItemStack> ITEM_STACK_RENDER_CONTEXT = new ThreadLocal<>();

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
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("and"), AndItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("or"), OrItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("not"), NotItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("count"), CountItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("durability"), DurabilityItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("enchantment"), EnchantmentItemPredicate.SERIALIZER);
        ItemPredicateRegistry.registerItemPredicate(Fusion.identifier("potion"), PotionItemPredicate.SERIALIZER);
    }

    public static void finalizeRegistries(){
        TextureTypeRegistryImpl.finalizeRegistration();
        ModelTypeRegistryImpl.finalizeRegistration();
        ConnectionPredicateRegistryImpl.finalizeRegistration();
        ItemPredicateRegistry.finalizeRegistration();
    }

    public static Optional<BlockRenderLayer> getChunkLayer(BaseTextureData.RenderType renderType){
        if(renderType == null)
            return Optional.empty();
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
        return Optional.of(material);
    }

    private static String fusionVersion;

    public static String getFusionVersion(){
        if(fusionVersion == null){
            String version = Fusion.class.getDeclaredAnnotation(Mod.class).version();
            if(!version.matches("\\d+\\.\\d+\\.\\d+"))
                version = version.substring(0, version.length() - version.replaceFirst("\\d+\\.\\d+\\.\\d+\\D", "").length() - 1);
            fusionVersion = version;
        }
        return fusionVersion;
    }
}
