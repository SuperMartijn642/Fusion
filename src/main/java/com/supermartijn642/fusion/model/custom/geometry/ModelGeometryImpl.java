package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.util.ChunkRenderTypeHelper;
import com.supermartijn642.fusion.util.CullingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class ModelGeometryImpl implements ModelGeometry {

    public static ModelGeometry of(UnbakedModel model){
        if(model instanceof BlockModel)
            return CuboidModelGeometry.of((BlockModel)model);
        return new ModelGeometryImpl(model);
    }

    public static TextureSlots createTextureSlots(MaterialKeyResolver materialResolver){
        return new TextureSlots(Map.of()) {
            @Override
            public @Nullable Material getMaterial(String reference){
                TextureAtlasSprite sprite = materialResolver.get(reference, false);
                if(ModelMaterial.isMissingSprite(sprite))
                    return null;
                return new Material(sprite.atlasLocation(), sprite.contents().name());
            }
        };
    }

    public static MaterialKeyResolver fromKeyLookup(Function<String,Either<String,ModelMaterial>> lookup,
                                                    Function<ModelMaterial,TextureAtlasSprite> materialResolver,
                                                    Consumer<String> reportMissing,
                                                    Consumer<List<String>> reportCircular){
        Map<String,TextureAtlasSprite> resolvedMaterials = new HashMap<>();
        return (key, required) -> {
            // Check if the key has already been resolved
            TextureAtlasSprite resolved = resolvedMaterials.get(key);
            if(resolved != null)
                return resolved;
            // Resolve the key
            List<String> encounteredKeys = new ArrayList<>();
            while(true){
                encounteredKeys.add(key);
                Either<String,ModelMaterial> next = lookup.apply(key);
                if(next == null){
                    if(required)
                        reportMissing.accept(key);
                    break;
                }
                if(next.isRight()){
                    TextureAtlasSprite material = materialResolver.apply(next.right());
                    for(String encounteredKey : encounteredKeys)
                        resolvedMaterials.put(encounteredKey, material);
                    return material;
                }
                key = next.left();
                if(encounteredKeys.contains(key)){
                    encounteredKeys.add(key);
                    reportCircular.accept(Collections.unmodifiableList(encounteredKeys));
                    break;
                }
                TextureAtlasSprite previouslyResolved = resolvedMaterials.get(key);
                if(previouslyResolved != null){
                    for(String encounteredKey : encounteredKeys)
                        resolvedMaterials.put(encounteredKey, previouslyResolved);
                    return previouslyResolved;
                }
            }
            TextureAtlasSprite missing = materialResolver.apply(ModelMaterial.missingBlockAtlas());
            for(String encounteredKey : encounteredKeys)
                resolvedMaterials.put(encounteredKey, missing);
            return missing;
        };
    }

    private static final BakedModel DUMMY_BAKED_MODEL = new BakedModel() {
        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
            return List.of();
        }

        @Override
        public boolean useAmbientOcclusion(){
            return true;
        }

        @Override
        public boolean isGui3d(){
            return false;
        }

        @Override
        public boolean usesBlockLight(){
            return false;
        }

        @Override
        public TextureAtlasSprite getParticleIcon(){
            ModelMaterial material = ModelMaterial.missingBlockAtlas();
            return Minecraft.getInstance().getTextureAtlas(material.atlas()).apply(material.texture());
        }

        @Override
        public ItemTransforms getTransforms(){
            return ItemTransforms.NO_TRANSFORMS;
        }
    };

    private final UnbakedModel model;

    ModelGeometryImpl(UnbakedModel model){
        this.model = model;
    }

    @Override
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialKeyResolver materialResolver){
        // Create dummy texture slots instance
        TextureSlots textureSlots = createTextureSlots(materialResolver);
        // Create dummy sprite getter
        SpriteGetter spriteGetter = new SpriteGetter() {
            @Override
            public TextureAtlasSprite get(Material material){
                return materialResolver.get(material.texture().toString());
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String reference){
                return materialResolver.get(reference);
            }
        };
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public UnbakedModel getModel(ResourceLocation location){
                return null;
            }

            @Override
            public SpriteGetter sprites(){
                return spriteGetter;
            }

            @Override
            public BakedModel bake(ResourceLocation model, ModelState modelState){
                return DUMMY_BAKED_MODEL;
            }

            @Override
            public ModelDebugName rootName(){
                return () -> "unknown";
            }
        };

        // Bake the model
        BakedModel baked;
        try{
            baked = this.model.bake(textureSlots, modelBaker, transformation.toModelState(), true, true, ItemTransforms.NO_TRANSFORMS);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking model of class '" + this.model.getClass().getName() + "'!", e);
        }

        // Create dummy random
        RandomSource random = RandomSource.createNewThreadLocalInstance();

        // Collect all quads from the model
        for(RenderType renderType : baked.getRenderTypes(Blocks.AIR.defaultBlockState(), random, ModelData.EMPTY)){
            // Skip non-chunk render types
            if(!ChunkRenderTypeHelper.isChunkRenderType(renderType))
                continue;
            for(Direction cullDirection : CullingHelper.cullDirections()){
                baked.getQuads(Blocks.AIR.defaultBlockState(), cullDirection, random, ModelData.EMPTY, renderType).forEach(q -> {
                    MutableQuad mutableQuad = MutableQuad.create(q);
                    mutableQuad.chunkRenderType(renderType);
                    consumer.consume(mutableQuad, cullDirection, PropertyGetter.empty());
                });
            }
        }
    }
}
