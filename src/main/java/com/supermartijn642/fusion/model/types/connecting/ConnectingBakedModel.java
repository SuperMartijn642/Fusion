package com.supermartijn642.fusion.model.types.connecting;

import com.mojang.math.Transformation;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.predicate.ConnectionPredicate;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureData;
import com.supermartijn642.fusion.api.texture.data.ConnectingTextureLayout;
import com.supermartijn642.fusion.model.WrappedBakedModel;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureLayoutHelper;
import com.supermartijn642.fusion.texture.types.connecting.ConnectingTextureSprite;
import com.supermartijn642.fusion.util.TextureAtlases;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadView;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.EncodingFormat;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created 27/04/2023 by SuperMartijn642
 */
public class ConnectingBakedModel extends WrappedBakedModel {

    private final Transformation modelRotation;
    private final Map<ResourceLocation,ConnectionPredicate> predicates;
    // [cullface][hashcode * 6]
    private final IntObjectMap<List<QuadView>> quadCache = new IntObjectHashMap<>();

    public ConnectingBakedModel(BakedModel original, Transformation modelRotation, Map<ResourceLocation,ConnectionPredicate> predicates){
        super(original);
        this.modelRotation = modelRotation;
        this.predicates = predicates;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        SurroundingBlockData data = blockView == null || pos == null ? null : this.getModelData(blockView, pos, state);
        int hashCode = data == null ? 0 : data.hashCode();

        // Get the quads from the cache
        List<QuadView> quads;
        synchronized(this.quadCache){
            quads = this.quadCache.get(hashCode);
        }

        if(quads == null){
            // Capture the quads
            List<QuadView> captures = new ArrayList<>();
            context.pushTransform(quad -> {
                //noinspection UnstableApiUsage
                MutableQuadViewImpl copy = new MutableQuadViewImpl() {
                    {
                        //noinspection UnstableApiUsage
                        this.data = new int[EncodingFormat.TOTAL_STRIDE];
                    }

                    @Override
                    public void emitDirectly(){
                    }
                };
                //noinspection UnstableApiUsage
                copy.copyFrom(quad);
                captures.add(copy);
                return true;
            });

            // Transform the quads
            SpriteFinder spriteFinder = SpriteFinder.get(Minecraft.getInstance().getModelManager().getAtlas(TextureAtlases.getBlocks()));
            context.pushTransform(quad -> this.remapQuad(quad, spriteFinder.find(quad), data));

            // Submit the original model
            this.original.emitBlockQuads(blockView, state, pos, randomSupplier, context);

            // Pop the transforms
            context.popTransform();
            context.popTransform();

            // Cache the collected quads
            quads = Arrays.asList(captures.toArray(QuadView[]::new));
            synchronized(this.quadCache){
                this.quadCache.putIfAbsent(hashCode, quads);
            }
        }else{
            // Submit the quads
            QuadEmitter emitter = context.getEmitter();
            quads.forEach(quad -> {
                emitter.copyFrom(quad);
                emitter.emit();
            });
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        this.original.emitItemQuads(stack, randomSupplier, context);
    }

    protected boolean remapQuad(MutableQuadView quad, TextureAtlasSprite sprite, SurroundingBlockData surroundingBlocks){
        if(SpriteHelper.getTextureType(sprite) != DefaultTextureTypes.CONNECTING)
            return true;
        ConnectingTextureLayout layout = ((ConnectingTextureSprite)sprite).getLayout();

        // Mark the render type for the quad
        ConnectingTextureData.RenderType renderType = ((ConnectingTextureSprite)sprite).getRenderType();
        if(renderType != null)
            quad.material(FusionClient.getRenderTypeMaterial(renderType));

        // Adjust the uv
        ResourceLocation spriteIdentifier = sprite.contents() == null || sprite.contents().name() == null ? ConnectingModelType.DEFAULT_CONNECTION_KEY : sprite.contents().name();
        if(!this.predicates.containsKey(spriteIdentifier))
            spriteIdentifier = ConnectingModelType.DEFAULT_CONNECTION_KEY;
        SurroundingBlockData.SideConnections connections = surroundingBlocks.getConnections(spriteIdentifier, quad.nominalFace());
        int[] uv = ConnectingTextureLayoutHelper.getStatePosition(layout, connections);
        adjustVertexDataUV(quad, uv[0], uv[1], sprite);

        // Create a new quad
        return true;
    }

    private static void adjustVertexDataUV(MutableQuadView quad, int newU, int newV, TextureAtlasSprite sprite){
        for(int i = 0; i < 4; i++){
            float width = sprite.getU1() - sprite.getU0();
            float u = quad.u(i) + width * newU;

            float height = sprite.getV1() - sprite.getV0();
            float v = quad.v(i) + height * newV;
            quad.uv(i, u, v);
        }
    }

    public SurroundingBlockData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state){
        return SurroundingBlockData.create(level, pos, this.modelRotation, this.predicates);
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public boolean isCustomRenderer(){
        return super.isCustomRenderer();
    }

    @Override
    public ItemTransforms getTransforms(){
        return super.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides(){
        return ItemOverrides.EMPTY;
    }
}
