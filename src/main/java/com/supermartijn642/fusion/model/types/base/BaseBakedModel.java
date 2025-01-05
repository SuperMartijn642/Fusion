package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.model.MutableQuad;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureSprite;
import com.supermartijn642.fusion.texture.types.continuous.ContinuousTextureType;
import com.supermartijn642.fusion.texture.types.random.RandomTextureSprite;
import com.supermartijn642.fusion.texture.types.random.RandomTextureType;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.block.model.BakedOverrides;
import net.minecraft.client.renderer.block.model.BakedQuad;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel {

    /*
     * Quads are tagged if they need further processing.
     * The tag consists of:
     *  - 4 bits to indicate texture type
     *  - 8 bits to indicate sprite index
     */

    private final Mesh blockMesh, itemMesh;
    private final List<TextureAtlasSprite> sprites;
    private final boolean hasSpecialQuads;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    private final BakedOverrides overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms, BakedOverrides overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.usesBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create the block mesh
        MeshBuilder builder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
        QuadEmitter emitter = builder.getEmitter();
        HashMap<TextureAtlasSprite,Integer> sprites = new HashMap<>();
        boolean hasSpecialQuads = false;
        for(BaseModelQuad quad : quads){
            RenderMaterial material = FusionClient.getRenderTypeMaterial(hasAmbientOcclusion, quad.renderType(), quad.emissive());
            emitter.fromVanilla(quad.bakedQuad(), material, quad.cullDirection());
            // Tag quads which need additional processing
            if(quad.textureType() == DefaultTextureTypes.RANDOM || quad.textureType() == DefaultTextureTypes.CONTINUOUS){
                int type = quad.textureType() == DefaultTextureTypes.RANDOM ? 2 : 3;
                // Give each sprite a unique index
                int spriteIndex = sprites.computeIfAbsent(quad.bakedQuad().getSprite(), o -> sprites.size());
                // Pack the type and sprite index into the tag
                emitter.tag(type | (spriteIndex << 4));
                hasSpecialQuads = true;
            }
            emitter.emit();
        }
        this.blockMesh = builder.build();
        this.sprites = sprites.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(Map.Entry::getKey).toList();
        this.hasSpecialQuads = hasSpecialQuads;

        // Create the item mesh
        builder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
        emitter = builder.getEmitter();
        for(BaseModelQuad quad : quads){
            RenderMaterial material = FusionClient.getRenderTypeMaterial(null, null, quad.emissive());
            emitter.fromVanilla(quad.bakedQuad(), material, quad.cullDirection());
            emitter.emit();
        }
        this.itemMesh = builder.build();
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random){
        return List.of();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, RenderContext context){
        if(!this.hasSpecialQuads){
            this.blockMesh.outputTo(context.getEmitter());
            return;
        }

        // If a quad is going to get culled anyway, don't bother processing it
        boolean[] culledFaces = {
            context.isFaceCulled(Direction.DOWN),
            context.isFaceCulled(Direction.UP),
            context.isFaceCulled(Direction.NORTH),
            context.isFaceCulled(Direction.SOUTH),
            context.isFaceCulled(Direction.WEST),
            context.isFaceCulled(Direction.EAST)
        };

        // Process special texture type quads
        MutableQuad mutableQuad = new MutableQuad();
        context.pushTransform(
            quad -> {
                if(quad.tag() != 0){
                    // Ignore the quad if it will be culled anyway
                    Direction cullFace = quad.cullFace();
                    if(cullFace != null && culledFaces[cullFace.ordinal()])
                        return false;

                    // Unpack type and sprite index from the tag
                    int tag = quad.tag();
                    int type = quad.tag() & ((1 << 4) - 1);
                    int spriteIndex = (tag >> 4) & ((1 << 8) - 1);

                    // Get the sprite
                    TextureAtlasSprite sprite = this.sprites.get(spriteIndex);

                    // TODO fix this workaround
                    quad.tag((int)Math.floor((sprite.u1 + sprite.u0) / 2 * 65535) | (int)Math.floor((sprite.v1 + sprite.v0) / 2 * 65535) << 16);

                    // Handle random texture type
                    if(type == 2){
                        mutableQuad.set(quad);
                        RandomTextureType.processQuad(mutableQuad, pos, quad.nominalFace(), randomSupplier, (RandomTextureSprite)sprite);
                        return true;
                    }
                    // Handle continuous texture type
                    if(type == 3){
                        mutableQuad.set(quad);
                        ContinuousTextureType.processQuad(mutableQuad, pos, quad.nominalFace(), (ContinuousTextureSprite)sprite);
                        return true;
                    }
                }
                return true;
            }
        );
        this.blockMesh.outputTo(context.getEmitter());
        context.popTransform();
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier, RenderContext context){
        this.itemMesh.outputTo(context.getEmitter());
    }

    @Override
    public boolean useAmbientOcclusion(){
        return this.hasAmbientOcclusion;
    }

    @Override
    public boolean isGui3d(){
        return this.isGui3d;
    }

    @Override
    public boolean usesBlockLight(){
        return this.usesBlockLight;
    }

    @Override
    public boolean isCustomRenderer(){
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(){
        return this.particleIcon;
    }

    @Override
    public ItemTransforms getTransforms(){
        return this.transforms;
    }

    @Override
    public BakedOverrides overrides(){
        return this.overrides;
    }
}
