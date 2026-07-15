package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created 02/01/2025 by SuperMartijn642
 */
@Mixin(BlockRenderer.class)
public class BlockRendererMixinRubidium {

    @Unique
    private final ModelsByRandomOffset modelsByRandomOffset = new ModelsByRandomOffset();

    @Shadow
    private boolean renderModel(IBlockDisplayReader level, BlockState state, BlockPos pos, IBakedModel model, ChunkModelBuffers buffers, boolean cull, long seed, IModelData modelData){
        throw new AssertionError();
    }

    @Inject(
        method = "renderModel",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void renderModel(IBlockDisplayReader level, BlockState state, BlockPos pos, IBakedModel model, ChunkModelBuffers buffers, boolean cull, long seed, IModelData modelData, CallbackInfoReturnable<Boolean> ci){
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        AtomicBoolean rendered = new AtomicBoolean(false);
        this.modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    if(this.renderModel(level, state, pos, entry, buffers, cull, seed, modelData))
                        rendered.set(true);
                }
            );
        }finally{
            this.modelsByRandomOffset.reset();
        }
        ci.setReturnValue(rendered.get());
    }

    @ModifyVariable(
        method = "renderModel",
        at = @At(
            value = "FIELD",
            target = "Lme/jellysquid/mods/sodium/common/util/DirectionUtil;ALL_DIRECTIONS:[Lnet/minecraft/util/Direction;",
            shift = At.Shift.BEFORE
        ),
        remap = false
    )
    private Vector3d modifyRandomOffset(Vector3d original, IBlockDisplayReader level, BlockState state, BlockPos pos, IBakedModel model, ChunkModelBuffers buffers, boolean cull, long seed, IModelData modelData){
        if(!(model instanceof ModelsByRandomOffset.Entry))
            return original;
        ModelsByRandomOffset.Entry entry = (ModelsByRandomOffset.Entry)model;
        Vector3f offset = entry.getOffset();
        return new Vector3d(offset.x(), offset.y(), offset.z());
    }

    @ModifyVariable(
        method = "renderQuad",
        at = @At(
            value = "INVOKE_ASSIGN",
            target = "Lme/jellysquid/mods/sodium/client/model/quad/blender/BiomeColorBlender;getColors(Lnet/minecraft/client/renderer/color/IBlockColor;Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lme/jellysquid/mods/sodium/client/model/quad/ModelQuadView;)[I"
        ),
        remap = false
    )
    private int[] getBlockTint(int[] colors, IBlockDisplayReader level, BlockState state, BlockPos pos, ModelVertexSink sink, Vector3d offset, IBlockColor colorProvider, BakedQuad quad){
        // In case texture has a custom tinting set, replace the original tinting
        if(quad.getTintIndex() == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData){
                BaseTextureData.QuadTinting tinting = ((BaseTextureData)textureInstance.getCustomData()).getTinting();
                if(tinting != null){
                    int color = ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, state, level, pos));
                    colors = new int[]{color, color, color, color};
                }
            }
        }
        return colors;
    }
}
