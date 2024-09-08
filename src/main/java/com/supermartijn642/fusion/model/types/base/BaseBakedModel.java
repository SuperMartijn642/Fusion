package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.FusionClient;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseBakedModel implements BakedModel, FabricBakedModel {

    private static final RenderMaterial ITEM_MATERIAL = RendererAccess.INSTANCE.getRenderer().materialFinder().find();

    private final Mesh blockMesh, itemMesh;
    private final boolean hasAmbientOcclusion;
    private final boolean isGui3d;
    private final boolean usesBlockLight;
    private final TextureAtlasSprite particleIcon;
    private final ItemTransforms transforms;
    private final ItemOverrides overrides;

    public BaseBakedModel(List<BaseModelQuad> quads, boolean hasAmbientOcclusion, boolean isGui3d, boolean usesBlockLight, TextureAtlasSprite particleIcon, ItemTransforms transforms, ItemOverrides overrides){
        this.hasAmbientOcclusion = hasAmbientOcclusion;
        this.isGui3d = isGui3d;
        this.usesBlockLight = usesBlockLight;
        this.particleIcon = particleIcon;
        this.transforms = transforms;
        this.overrides = overrides;

        // Create block and item meshes from the quads
        MeshBuilder builder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
        QuadEmitter emitter = builder.getEmitter();
        for(BaseModelQuad quad : quads){
            RenderMaterial material = FusionClient.getRenderTypeMaterial(hasAmbientOcclusion, quad.renderType(), quad.emissive());
            emitter.fromVanilla(quad.bakedQuad(), material, quad.cullDirection());
            if(quad.lightEmission() != null){
                for(int i = 0; i < 4; i++){
                    int sky = Math.max(quad.lightEmission(), LightTexture.sky(emitter.lightmap(i)));
                    int block = Math.max(quad.lightEmission(), LightTexture.block(emitter.lightmap(i)));
                    emitter.lightmap(i, LightTexture.pack(sky, block));
                }
            }
            emitter.emit();
        }
        this.blockMesh = builder.build();
        builder = RendererAccess.INSTANCE.getRenderer().meshBuilder();
        emitter = builder.getEmitter();
        for(BaseModelQuad quad : quads){
            emitter.fromVanilla(quad.bakedQuad(), ITEM_MATERIAL, quad.cullDirection());
            if(quad.lightEmission() != null){
                for(int i = 0; i < 4; i++){
                    int sky = Math.max(quad.lightEmission(), LightTexture.sky(emitter.lightmap(i)));
                    int block = Math.max(quad.lightEmission(), LightTexture.block(emitter.lightmap(i)));
                    emitter.lightmap(i, LightTexture.pack(sky, block));
                }
            }
            emitter.emit();
        }
        this.itemMesh = builder.build();
    }

    @Override
    public boolean isVanillaAdapter(){
        return false;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, Random random){
        return List.of();
    }

    @Override
    public void emitBlockQuads(BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<Random> randomSupplier, RenderContext context){
        context.meshConsumer().accept(this.blockMesh);
    }

    @Override
    public void emitItemQuads(ItemStack stack, Supplier<Random> randomSupplier, RenderContext context){
        context.meshConsumer().accept(this.itemMesh);
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
    public ItemOverrides getOverrides(){
        return this.overrides;
    }
}
