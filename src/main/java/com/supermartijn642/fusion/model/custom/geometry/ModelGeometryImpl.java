package com.supermartijn642.fusion.model.custom.geometry;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.supermartijn642.fusion.Fusion;
import com.supermartijn642.fusion.api.model.custom.ModelMaterial;
import com.supermartijn642.fusion.api.model.custom.ModelTransform;
import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.FusionBlockModelData;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.util.EmptyBlockAndTintGetter;
import com.supermartijn642.fusion.util.IdentifierUtil;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created 03/05/2026 by SuperMartijn642
 */
public class ModelGeometryImpl implements ModelGeometry {

    public static ModelGeometry of(UnbakedModel model){
        if(model instanceof BlockModel)
            return CuboidModelGeometry.of((BlockModel)model);
        return new ModelGeometryImpl(model);
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
        public boolean isCustomRenderer(){
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

        @Override
        public ItemOverrides getOverrides(){
            return ItemOverrides.EMPTY;
        }
    };

    private final UnbakedModel model;

    ModelGeometryImpl(UnbakedModel model){
        this.model = model;
    }

    @Override
    public void bake(QuadConsumer consumer, ModelTransform transformation, MaterialResolver materialResolver){
        // Create dummy sprite getter
        Function<Material,TextureAtlasSprite> spriteGetter = material -> materialResolver.get(material.texture().toString());
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public UnbakedModel getModel(ResourceLocation identifier){
                return FusionBlockModelData.missingModel.left();
            }

            @Override
            public BakedModel bake(ResourceLocation model, ModelState modelState){
                return DUMMY_BAKED_MODEL;
            }
        };

        // Bake the model
        BakedModel baked;
        try{
            ResourceLocation identifier = this.model instanceof BlockModel && !((BlockModel)this.model).name.isEmpty() && IdentifierUtil.isValidIdentifier(((BlockModel)this.model).name) ?
                new ResourceLocation(((BlockModel)this.model).name) :
                Fusion.identifier("unknown_geometry");
            baked = this.model.bake(modelBaker, spriteGetter, transformation.toModelState(), identifier);
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking model of class '" + this.model.getClass().getName() + "'!", e);
        }
        if(baked == null)
            return;

        // Collect all quads from the model
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().tesselateBlock(
            EmptyBlockAndTintGetter.INSTANCE,
            new SimpleBakedModel(List.of(), UnknownModelType.EMPTY_CULLED_QUADS, false, false, false, null, ItemTransforms.NO_TRANSFORMS, ItemOverrides.EMPTY) {
                @Override
                public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
                    context.pushTransform(quad -> {
                        MutableQuad mutableQuad = MutableQuad.create().copyFrapiQuad(quad);
                        consumer.consume(mutableQuad, quad.cullFace(), PropertyGetter.empty());
                        return false;
                    });
                    baked.emitBlockQuads(blockView, state, pos, randomSupplier, context);
                    context.popTransform();
                }

                @Override
                public boolean isVanillaAdapter(){
                    return false;
                }
            },
            Blocks.AIR.defaultBlockState(),
            BlockPos.ZERO,
            new PoseStack(),
            VertexMultiConsumer.create(new VertexConsumer[0]),
            false,
            RandomSource.createNewThreadLocalInstance(),
            42,
            OverlayTexture.NO_OVERLAY
        );
    }
}
