package com.supermartijn642.fusion.mixin.rubidium;

import com.supermartijn642.fusion.api.texture.SpriteHelper;
import com.supermartijn642.fusion.api.texture.custom.TextureInstance;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.model.modifiers.block.BlockModelModifierBakedModel;
import com.supermartijn642.fusion.model.modifiers.block.ModelsByRandomOffset;
import com.supermartijn642.fusion.texture.QuadTintingHelper;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.vertex.type.ChunkVertexBufferBuilder;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3fc;
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
    @Unique
    private BlockRenderContext dummyRenderContext;
    @Unique
    private final BlockPos.MutableBlockPos dummyOrigin = new BlockPos.MutableBlockPos();

    @Shadow
    private boolean renderModel(BlockRenderContext ctx, ChunkModelBuilder buffers) {
        throw new AssertionError();
    }

    @Inject(
        method = "renderModel",
        at = @At("HEAD"),
        cancellable = true,
        remap = false
    )
    private void renderModel(BlockRenderContext context, ChunkModelBuilder buffers, CallbackInfoReturnable<Boolean> ci){
        BakedModel model = context.model();
        if(!(model instanceof BlockModelModifierBakedModel))
            return;
        BlockAndTintGetter level = context.world();
        if(this.dummyRenderContext == null || this.dummyRenderContext.world() != level)
            this.dummyRenderContext = new BlockRenderContext(level);
        Vector3fc origin = context.origin();
        this.dummyOrigin.set(origin.x(), origin.y(), origin.z());
        BlockPos pos = context.pos();
        BlockState state = context.state();
        long seed = context.seed();
        ModelData modelData = context.data();
        RenderType renderType = context.layer();
        AtomicBoolean rendered = new AtomicBoolean(false);
        this.modelsByRandomOffset.setContext(pos, state.getOffset(level, pos));
        try{
            ((BlockModelModifierBakedModel)model).collectByOffset(this.modelsByRandomOffset, level, pos, state);
            this.modelsByRandomOffset.foreach(
                entry -> {
                    this.dummyRenderContext.update(pos, this.dummyOrigin, state, entry, seed, modelData, renderType);
                    if(this.renderModel(this.dummyRenderContext, buffers))
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
            target = "Lme/jellysquid/mods/sodium/common/util/DirectionUtil;ALL_DIRECTIONS:[Lnet/minecraft/core/Direction;",
            shift = At.Shift.BEFORE
        ),
        remap = false
    )
    private Vec3 modifyRandomOffset(Vec3 original, BlockRenderContext context, ChunkModelBuilder buffers){
        if(!(context.model() instanceof ModelsByRandomOffset.Entry entry))
            return original;
        Vector3fc offset = entry.getOffset();
        return new Vec3(offset.x(), offset.y(), offset.z());
    }

    @ModifyVariable(
        method = "writeGeometry",
        at = @At("HEAD"),
        ordinal = 0,
        remap = false
    )
    private int[] getBlockTint(int[] colors, BlockRenderContext ctx, ChunkVertexBufferBuilder vertexBuffer, IndexBufferBuilder indexBuffer, Vec3 offset, ModelQuadView quad){
        // In case texture has a custom tinting set, replace the original tinting
        if(((BakedQuad)quad).tintIndex == 39216){
            TextureAtlasSprite sprite = quad.getSprite();
            TextureInstance<?> textureInstance = SpriteHelper.getTextureInstance(sprite);
            if(textureInstance != null && textureInstance.getCustomData() instanceof BaseTextureData data){
                BaseTextureData.QuadTinting tinting = data.getTinting();
                if(tinting != null){
                    int color = ColorARGB.toABGR(QuadTintingHelper.getColor(tinting, ctx.state(), ctx.world(), ctx.pos()));
                    colors = new int[]{color, color, color, color};
                }
            }
        }
        return colors;
    }
}
