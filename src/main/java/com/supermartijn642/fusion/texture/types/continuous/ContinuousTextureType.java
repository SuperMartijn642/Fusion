package com.supermartijn642.fusion.texture.types.continuous;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.blaze3d.platform.NativeImage;
import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.api.texture.TextureType;
import com.supermartijn642.fusion.api.texture.custom.*;
import com.supermartijn642.fusion.api.texture.types.base.BaseTextureData;
import com.supermartijn642.fusion.api.texture.types.continuous.ContinuousTextureData;
import com.supermartijn642.fusion.api.util.PropertyStore;
import com.supermartijn642.fusion.api.util.UserErrorException;
import com.supermartijn642.fusion.texture.types.base.BaseTextureType;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class ContinuousTextureType implements TextureType<ContinuousTextureData,ContinuousTextureData> {

    @Override
    public void createTexture(TextureOutput<ContinuousTextureData> output, TextureCreationContext context, ContinuousTextureData data) throws UserErrorException{
        // Calculate frame size
        int frameWidth = context.getImageWidth(), frameHeight = context.getImageHeight();
        AnimationMetadataSection animationMetadata = context.getAnimationMetadata();
        if(animationMetadata != null){
            if(animationMetadata.frameWidth().isEmpty() && animationMetadata.frameHeight().isEmpty()){
                // Use the expected aspect ratio for the layout
                float tileSize = Math.min((float)context.getImageWidth() / data.getColumns(), (float)context.getImageHeight() / data.getRows());
                frameWidth = (int)(tileSize * data.getColumns());
                frameHeight = (int)(tileSize * data.getRows());
            }else{
                if(animationMetadata.frameWidth().isPresent())
                    frameWidth = animationMetadata.frameWidth().get();
                if(animationMetadata.frameHeight().isPresent())
                    frameHeight = animationMetadata.frameHeight().get();
            }
        }

        // Do frame size checks
        if(frameWidth == 0 || frameHeight == 0)
            throw new UserErrorException("Image must not be empty!");
        if(context.getImageWidth() % frameWidth != 0 || context.getImageHeight() % frameHeight != 0)
            throw new UserErrorException("Image size " + context.getImageWidth() + "x" + context.getImageHeight() + " is not a multiple of frame size " + frameWidth + "x" + frameHeight + "!");

        // Create sprite
        SpriteImageSource image = SpriteImageSource.vanilla(
            context.getImage(),
            context.getAnimationMetadata(),
            frameWidth,
            frameHeight
        );
        output.createSprite()
            .image(image)
            .submit();
        // Create a sprite consisting of just the first tile
        int tileWidth = frameWidth / data.getColumns();
        int tileHeight = frameHeight / data.getRows();
        if(tileWidth != 0)
            tileWidth = Math.min(1 << 31 - Integer.numberOfLeadingZeros(tileWidth - 1), frameWidth);
        if(tileHeight != 0)
            tileHeight = Math.min(1 << 31 - Integer.numberOfLeadingZeros(tileHeight - 1), frameHeight);
        NativeImage defaultTileImage = ImageHelper.createCropFramed(context.getImage(), 0, 0, tileWidth, tileHeight, frameWidth, frameHeight, false);
        SpriteImageSource defaultTileImageSource;
        if(animationMetadata == null)
            defaultTileImageSource = SpriteImageSource.constant(defaultTileImage);
        else{
            List<SpriteImageSource.AnimationFrame> frames;
            int frameColumns = context.getImageWidth() / frameWidth;
            int frameRows = context.getImageHeight() / frameHeight;
            if(animationMetadata.frames().isEmpty()){
                frames = new ArrayList<>(frameColumns * frameRows);
                for(int row = 0; row < frameRows; row++){
                    for(int column = 0; column < frameColumns; column++){
                        frames.add(SpriteImageSource.AnimationFrame.of(column * tileWidth, row * tileHeight, animationMetadata.defaultFrameTime()));
                    }
                }
            }else{
                frames = new ArrayList<>(animationMetadata.frames().get().size());
                for(AnimationFrame frame : animationMetadata.frames().get()){
                    frames.add(SpriteImageSource.AnimationFrame.of(
                        (frame.index() % frameColumns) * tileWidth,
                        frame.index() / frameColumns * tileHeight,
                        frame.timeOr(animationMetadata.defaultFrameTime())
                    ));
                }
            }
            defaultTileImageSource = SpriteImageSource.animated(defaultTileImage, tileWidth, tileHeight, frames, animationMetadata.interpolatedFrames());
        }
        output.createSprite()
            .image(defaultTileImageSource)
            .markDefaultSprite()
            .submit();

        // Set custom texture data
        output.setCustomData(data);
    }

    @Override
    public @Nullable BlockStateQuadProcessor<?> initializeBlockStateModelQuad(MutableQuad quad, SpriteInstance sprite, ContinuousTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Create processor
        return new BlockStateQuadProcessor<BlockPos>() {
            @Override
            public BlockPos extractState(@Nullable BlockAndTintGetter level, @Nullable BlockPos pos, @Nullable BlockState state, Supplier<RandomSource> randomSupplier, PropertyStore properties){
                return pos == null ? BlockPos.ZERO : pos;
            }

            @Override
            public Object createGeometryKey(BlockPos state, PropertyStore properties){
                return Triple.of(DefaultTextureTypes.CONTINUOUS, sprite, state);
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, BlockPos pos, PropertyStore properties){
                // TODO account for the orientation of the texture and quad
                // Determine which tile to use
                Direction side = quad.facing();
                Direction.Axis axis = side.getAxis();
                int x = axis == Direction.Axis.X ? pos.getZ() : pos.getX();
                int y = axis == Direction.Axis.X ? -pos.getY() : axis == Direction.Axis.Y ? -pos.getZ() - 1 : -pos.getY();
                if(side == Direction.NORTH || side == Direction.EAST)
                    x = -x - 1;
                if(side == Direction.UP)
                    y = -y - 1;
                x = x < 0 ? ((x % data.getColumns()) + data.getColumns()) % data.getColumns() : x % data.getColumns();
                y = y < 0 ? ((y % data.getRows()) + data.getRows()) % data.getRows() : y % data.getRows();
                // Adjust the quad's uv
                SpriteInstance newSprite = sprite.getTexture().getSprites().get(0);
                float oldWidth = sprite.getU1() - sprite.getU0();
                float oldHeight = sprite.getV1() - sprite.getV0();
                float newWidth = (newSprite.getU1() - newSprite.getU0()) / data.getColumns();
                float newHeight = (newSprite.getV1() - newSprite.getV0()) / data.getRows();
                for(int i = 0; i < 4; i++){
                    quad.uv(
                        i,
                        newSprite.getU0() + (quad.u(i) - sprite.getU0()) / oldWidth * newWidth + x * newWidth,
                        newSprite.getV0() + (quad.v(i) - sprite.getV0()) / oldHeight * newHeight + y * newHeight
                    );
                }
                quad.emit();
            }
        };
    }

    @Override
    public @Nullable ItemQuadProcessor<?> initializeItemModelQuad(MutableQuad quad, SpriteInstance sprite, ContinuousTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);
        // No processing for item quads
        return null;
    }

    private static final int MAX_SIZE = 32; // Two chunks of size, blame resource packs if they blow up the texture atlas

    @Override
    public ContinuousTextureData deserialize(JsonObject json) throws JsonParseException{
        // Deserialize base properties
        BaseTextureData base = DefaultTextureTypes.BASE.deserialize(json);
        // Copy base properties
        ContinuousTextureData.Builder builder = ContinuousTextureData.builder();
        builder.renderType(base.getRenderType());
        builder.emissive(base.isEmissive());
        builder.tinting(base.getTinting());
        // Rows
        if(json.has("rows")){
            if(!json.get("rows").isJsonPrimitive() || !json.getAsJsonPrimitive("rows").isNumber())
                throw new JsonParseException("Property 'rows' must be a number!");
            int rows = json.get("rows").getAsInt();
            if(rows <= 0 || rows > MAX_SIZE)
                throw new JsonParseException("Property 'rows' must be greater than zero and less than 10!");
            builder.rows(rows);
        }
        // Columns
        if(json.has("columns")){
            if(!json.get("columns").isJsonPrimitive() || !json.getAsJsonPrimitive("columns").isNumber())
                throw new JsonParseException("Property 'columns' must be a number!");
            int columns = json.get("columns").getAsInt();
            if(columns <= 0 || columns > MAX_SIZE)
                throw new JsonParseException("Property 'columns' must be greater than zero and less than 10!");
            builder.columns(columns);
        }
        return builder.build();
    }

    @Override
    public JsonObject serialize(ContinuousTextureData data){
        // Serialize base properties
        JsonObject json = DefaultTextureTypes.BASE.serialize(data);
        // Rows
        if(data.getRows() != 1)
            json.addProperty("rows", data.getRows());
        // Columns
        if(data.getColumns() != 1)
            json.addProperty("columns", data.getColumns());
        return json;
    }
}
