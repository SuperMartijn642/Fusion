package com.supermartijn642.fusion.texture.types.continuous;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.data.AnimationFrame;
import net.minecraft.client.resources.data.AnimationMetadataSection;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
            if(animationMetadata.frameWidth < 0 && animationMetadata.frameHeight < 0){
                // Use the expected aspect ratio for the layout
                float tileSize = Math.min((float)context.getImageWidth() / data.getColumns(), (float)context.getImageHeight() / data.getRows());
                frameWidth = (int)(tileSize * data.getColumns());
                frameHeight = (int)(tileSize * data.getRows());
            }else{
                if(animationMetadata.frameWidth >= 0)
                    frameWidth = animationMetadata.frameWidth;
                if(animationMetadata.frameHeight >= 0)
                    frameHeight = animationMetadata.frameHeight;
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
        BufferedImage defaultTileImage = ImageHelper.createCropFramed(context.getImage(), 0, 0, tileWidth, tileHeight, frameWidth, frameHeight);
        SpriteImageSource defaultTileImageSource;
        if(animationMetadata == null)
            defaultTileImageSource = SpriteImageSource.constant(defaultTileImage);
        else{
            List<SpriteImageSource.AnimationFrame> frames;
            int frameColumns = context.getImageWidth() / frameWidth;
            int frameRows = context.getImageHeight() / frameHeight;
            if(animationMetadata.animationFrames.isEmpty()){
                frames = new ArrayList<>(frameColumns * frameRows);
                for(int row = 0; row < frameRows; row++){
                    for(int column = 0; column < frameColumns; column++){
                        frames.add(SpriteImageSource.AnimationFrame.of(column * tileWidth, row * tileHeight, animationMetadata.getFrameTime()));
                    }
                }
            }else{
                frames = new ArrayList<>(animationMetadata.animationFrames.size());
                for(AnimationFrame frame : animationMetadata.animationFrames){
                    frames.add(SpriteImageSource.AnimationFrame.of(
                        (frame.getFrameIndex() % frameColumns) * tileWidth,
                        frame.getFrameIndex() / frameColumns * tileHeight,
                        frame.hasNoTime() ? animationMetadata.getFrameTime() : frame.getFrameTime()
                    ));
                }
            }
            defaultTileImageSource = SpriteImageSource.animated(defaultTileImage, tileWidth, tileHeight, frames, animationMetadata.isInterpolate());
        }
        output.createSprite()
            .image(defaultTileImageSource)
            .markDefaultSprite()
            .submit();

        // Set custom texture data
        output.setCustomData(data);
    }

    @Override
    public @Nullable QuadProcessor<?> initializeModelQuad(MutableQuad quad, SpriteInstance sprite, ContinuousTextureData data, PropertyStore properties){
        // Apply base texture properties
        BaseTextureType.applyProperties(quad, data);

        // Create processor
        TextureInstance<?> texture = sprite.getTexture();
        return new QuadProcessor<BlockPos>() {
            @Override
            public BlockPos extractState(Supplier<Random> randomSupplier, PropertyStore properties){
                return null;
            }

            @Override
            public BlockPos extractState(@Nullable IBlockAccess level, @Nullable BlockPos pos, @Nullable IBlockState state, Supplier<Random> randomSupplier, PropertyStore properties){
                return pos;
            }

            @Override
            public BlockPos extractState(ItemStack stack, Supplier<Random> randomSupplier, PropertyStore properties){
                return null;
            }

            @Override
            public @Nullable Object createGeometryKey(BlockPos state, PropertyStore properties){
                return null;
            }

            @Override
            public void processQuad(EmittableQuad quad, SpriteInstance sprite, BlockPos pos, PropertyStore properties){
                if(pos == null){
                    quad.emit();
                    return;
                }

                // Determine which tile to use
                EnumFacing side = quad.facing();
                EnumFacing.Axis axis = side.getAxis();
                int x = axis == EnumFacing.Axis.X ? pos.getZ() : pos.getX();
                int y = axis == EnumFacing.Axis.X ? -pos.getY() : axis == EnumFacing.Axis.Y ? -pos.getZ() - 1 : -pos.getY();
                if(side == EnumFacing.NORTH || side == EnumFacing.EAST)
                    x = -x - 1;
                if(side == EnumFacing.UP)
                    y = -y - 1;
                x = x < 0 ? ((x % data.getColumns()) + data.getColumns()) % data.getColumns() : x % data.getColumns();
                y = y < 0 ? ((y % data.getRows()) + data.getRows()) % data.getRows() : y % data.getRows();
                // Adjust the quad's uv
                SpriteInstance newSprite = texture.getSprites().get(0);
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
