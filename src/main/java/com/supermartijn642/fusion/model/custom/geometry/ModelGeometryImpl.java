package com.supermartijn642.fusion.model.custom.geometry;

import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableMesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.block.model.TextureSlots;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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

    public static TextureSlots createTextureSlots(MaterialResolver materialResolver){
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

    public static MaterialResolver fromKeyLookup(Function<String,Either<String,ModelMaterial>> lookup,
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
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialResolver materialResolver){
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
        MutableMesh mesh = Renderer.get().mutableMesh();
        QuadEmitter emitter = mesh.emitter();
        emitter.pushTransform(quadView -> {
            MutableQuad mutableQuad = MutableQuad.create().copyFrapiQuad(quadView);
            consumer.consume(mutableQuad, quadView.cullFace(), PropertyGetter.empty());
            return false;
        });
        baked.emitBlockQuads(emitter, EmptyBlockAndTintGetter.INSTANCE, Blocks.AIR.defaultBlockState(), BlockPos.ZERO, () -> random, dir -> false);
        emitter.popTransform();
    }
}
