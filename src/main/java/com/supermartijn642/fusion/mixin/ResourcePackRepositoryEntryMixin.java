package com.supermartijn642.fusion.mixin;

import com.supermartijn642.fusion.extensions.PackExtension;
import com.supermartijn642.fusion.resources.FusionPackMetadata;
import com.supermartijn642.fusion.resources.FusionPackMetadataSection;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.ResourcePackRepository;
import net.minecraft.client.resources.data.MetadataSerializer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Created 21/10/2024 by SuperMartijn642
 */
@Mixin(ResourcePackRepository.Entry.class)
public class ResourcePackRepositoryEntryMixin implements PackExtension {

    @Unique
    private static final MetadataSerializer METADATA_SERIALIZER = new MetadataSerializer();

    static{
        METADATA_SERIALIZER.registerMetadataSectionType(FusionPackMetadataSection.INSTANCE, FusionPackMetadataSection.Data.class);
    }

    @Final
    @Shadow
    private IResourcePack reResourcePack;
    @Final
    @Shadow(aliases = "this$0")
    private ResourcePackRepository repository;
    @Unique
    private FusionPackMetadata metadata;

    @Nullable
    @Override
    public FusionPackMetadata getFusionMetadata(){
        return this.metadata;
    }

    @Inject(
        method = "updateResourcePack",
        at = @At("HEAD")
    )
    public void parseFusionMetadata(CallbackInfo ci) throws IOException{
        FusionPackMetadataSection.Data data = this.reResourcePack.getPackMetadata(METADATA_SERIALIZER, FusionPackMetadataSection.INSTANCE.getSectionName());
        this.metadata = data == null ? null : data.metadata;
    }
}
