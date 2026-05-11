package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.CullableQuads;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.QuadAccess;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel, FabricBakedModel {

    public static final Direction[] CULL_DIRECTIONS = {null, Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};

    private final List<Part> parts;
    private final TextureAtlasSprite particleSprite;
    private final BlockModel.GuiLight guiLight;
    private final boolean isGui3d;
    private final ItemTransforms transforms;

    public BaseBakedModel(List<Part> parts, TextureAtlasSprite particleSprite, BlockModel.GuiLight guiLight, boolean isGui3d, ItemTransforms transforms){
        this.parts = parts;
        this.particleSprite = particleSprite;
        this.guiLight = guiLight;
        this.isGui3d = isGui3d;
        this.transforms = transforms;
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        QuadEmitter emitter = context.getEmitter();
        for(Part part : this.parts){
            for(Direction cullDirection : CULL_DIRECTIONS){
                // Set cull direction
                emitter.cullFace(cullDirection);

                MutableQuad mutableQuad = null;
                for(QuadAccess quad : part.quads().get(cullDirection)){
                    // Get the sprite instance
                    SpriteInstance sprite = SpriteHelper.getSpriteInstance(quad.sprite());
                    if(sprite == null){
                        quad.toFrapiQuad(emitter);
                        emitter.emit();
                        continue;
                    }

                    // Process special texture type quads
                    TextureType<?,?> textureType = sprite.getTexture().getTextureType();
                    if(textureType == DefaultTextureTypes.RANDOM){
                        if(mutableQuad == null)
                            mutableQuad = MutableQuad.create();
                        mutableQuad.copyFrom(quad);
                        RandomTextureType.processQuad(mutableQuad, pos, quad.facing(), randomSupplier, sprite);
                        mutableQuad.toFrapiQuad(emitter);
                        emitter.emit();
                    }else if(textureType == DefaultTextureTypes.CONTINUOUS){
                        if(mutableQuad == null)
                            mutableQuad = MutableQuad.create();
                        mutableQuad.copyFrom(quad);
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.facing(), sprite);
                        mutableQuad.toFrapiQuad(emitter);
                        emitter.emit();
                    }else{
                        quad.toFrapiQuad(emitter);
                        emitter.emit();
                    }
                }
            }
        }
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        // Emit quads
        QuadEmitter emitter = context.getEmitter();
        for(Part part : this.parts){
            for(QuadAccess quad : part.quads().all()){
                quad.toFrapiQuad(emitter);
                emitter.emit();
            }
        }
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction cullDirection, RandomSource random){
        return List.of();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleSprite;
    }

    @Override
    public boolean useAmbientOcclusion(){
        return true; // Ambient occlusion is handled by quads themselves
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight(){
        return this.guiLight.lightLikeBlock();
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public ItemOverrides getOverrides(){
        return ItemOverrides.EMPTY;
    }

    public record Part(CullableQuads quads) {
    }
}
