package com.supermartijn642.fusion.texture;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.supermartijn642.fusion.api.texture.custom.SpriteImageSource;
import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.texture.custom.SpriteImageSourceImpl;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.util.ARGB;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.IntStream;

/**
 * Created 27/03/2026 by SuperMartijn642
 */
public class FusionSpriteContents extends SpriteContents {

    private final SpriteImageSourceImpl imageSource;
    private final int imageWidth, imageHeight;
    private final int frameWidth, frameHeight;
    private final List<SpriteImageSource.AnimationFrame> uniqueFrames;
    private final List<FrameInfo> frameInfos;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public FusionSpriteContents(Identifier identifier, SpriteImageSourceImpl imageSource, List<MetadataSectionType.WithValue<?>> metadataSections, Optional<TextureMetadataSection> textureMetadata){
        super(identifier, new FrameSize(imageSource.getFrameWidth(), imageSource.getFrameHeight()), imageSource.getImage(), Optional.empty(), metadataSections, textureMetadata);
        this.imageSource = imageSource;
        this.imageWidth = imageSource.getImage().getWidth();
        this.imageHeight = imageSource.getImage().getHeight();
        this.frameWidth = imageSource.getFrameWidth();
        this.frameHeight = imageSource.getFrameHeight();
        // Create vanilla frame infos
        Map<Pair<Integer,Integer>,Integer> frameUVs = new HashMap<>();
        List<SpriteImageSource.AnimationFrame> uniqueFrames = new ArrayList<>();
        List<FrameInfo> frameInfos = new ArrayList<>(imageSource.getFrames().size());
        for(SpriteImageSource.AnimationFrame frame : imageSource.getFrames()){
            int uniqueFrameIndex = frameUVs.computeIfAbsent(Pair.of(frame.u(), frame.v()), p -> {
                uniqueFrames.add(frame);
                return uniqueFrames.size() - 1;
            });
            frameInfos.add(new FrameInfo(uniqueFrameIndex, frame.time()));
        }
        this.uniqueFrames = List.copyOf(uniqueFrames);
        this.frameInfos = List.copyOf(frameInfos);
    }

    @Override
    public boolean isTransparent(int uniqueFrameIndex, int x, int y){
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(0);
        x = (frame.u() + x) % this.imageWidth;
        y = (frame.v() + y) % this.imageHeight;
        return ARGB.alpha(this.imageSource.getImage().getPixel(x, y)) == 0;
    }

    @Override
    public void uploadFirstFrame(GpuTexture gpuTexture, int mipLevel){
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        this.uploadUniqueFrame(gpuTexture, mipLevel, 0, encoder);
    }

    @Override
    public boolean isAnimated(){
        return this.uniqueFrames.size() > 1;
    }

    @Override
    public IntList getUniqueFrames(){
        return IntList.of(IntStream.range(0, this.uniqueFrames.size()).toArray());
    }

    @Override
    public @Nullable AnimationState createAnimationState(GpuBufferSlice bufferSlice, int offset){
        GpuDevice gpuDevice = RenderSystem.getDevice();
        Int2ObjectMap<GpuTextureView> uniqueFrameGpuTextures = new Int2ObjectOpenHashMap<>();
        GpuBufferSlice[] gpuBufferSlices = new GpuBufferSlice[this.byMipLevel.length];

        // Create a gpu texture for each unique frame
        for(int uniqueFrameIndex : this.getUniqueFrames()){
            GpuTexture gpuTexture = gpuDevice.createTexture(
                () -> this.name + " animation frame " + uniqueFrameIndex,
                5,
                GpuFormat.RGBA8_UNORM,
                this.frameWidth,
                this.frameHeight,
                1,
                this.byMipLevel.length
            );
            CommandEncoder encoder = gpuDevice.createCommandEncoder();
            for(int mipLevel = 0; mipLevel < this.byMipLevel.length; mipLevel++)
                this.uploadUniqueFrame(gpuTexture, mipLevel, uniqueFrameIndex, encoder);
            uniqueFrameGpuTextures.put(uniqueFrameIndex, RenderSystem.getDevice().createTextureView(gpuTexture));
        }

        for(int mipLevel = 0; mipLevel < this.byMipLevel.length; mipLevel++)
            gpuBufferSlices[mipLevel] = bufferSlice.slice((long)mipLevel * offset, offset);

        AnimatedTexture animatedTexture = new AnimatedTexture(this.frameInfos, 0, this.imageSource.shouldInterpolateFrames());
        return new AnimationState(animatedTexture, uniqueFrameGpuTextures, gpuBufferSlices);
    }

    private void uploadUniqueFrame(GpuTexture destination, int mipLevel, int frameIndex, CommandEncoder encoder){
        GpuBufferSlice stagingBuffer = encoder.transientMemory().uploadStaging(this.byMipLevel[mipLevel].getPixelBytes(), 1, GpuBuffer.USAGE_COPY_SRC);
        SpriteImageSource.AnimationFrame frame = this.uniqueFrames.get(frameIndex);
        this.uploadToTexture(stagingBuffer, destination, mipLevel, 0, 0, frame.u(), frame.v(), this.frameWidth, this.frameHeight, encoder);
    }

    private void uploadToTexture(GpuBufferSlice source, GpuTexture destination, int mipLevel, int destinationX, int destinationY, int sourceX, int sourceY, int width, int height, CommandEncoder encoder){
        if(sourceX + width > this.imageWidth){
            int overflow = sourceX + width - this.imageWidth;
            this.uploadToTexture(source, destination, mipLevel, destinationX, destinationY, sourceX, sourceY, width - overflow, height, encoder);
            this.uploadToTexture(source, destination, mipLevel, destinationX + width - overflow, 0, 0, sourceY, overflow, height, encoder);
            return;
        }
        if(sourceY + height > this.imageHeight){
            int overflow = sourceY + height - this.imageHeight;
            this.uploadToTexture(source, destination, mipLevel, destinationX, destinationY, sourceX, sourceY, width, height - overflow, encoder);
            this.uploadToTexture(source, destination, mipLevel, destinationX, destinationY + height - overflow, sourceX, 0, width, overflow, encoder);
            return;
        }
        encoder.copyBufferToTexture(
            source,
            sourceX >> mipLevel,
            sourceY >> mipLevel,
            this.byMipLevel[mipLevel].getWidth(),
            this.byMipLevel[mipLevel].getHeight(),
            destination,
            destinationX >> mipLevel,
            destinationY >> mipLevel,
            width >> mipLevel,
            height >> mipLevel,
            mipLevel,
            0
        );
    }
}
