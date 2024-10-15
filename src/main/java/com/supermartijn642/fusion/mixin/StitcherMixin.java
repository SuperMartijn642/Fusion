package com.supermartijn642.fusion.mixin;

import net.minecraft.client.renderer.texture.Stitcher;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixes a bug with the stitcher not allocating enough space for a new texture and thus the texture never actually being added to the atlas.
 * <p>
 * Example:<br>
 * Assume the current atlas size is 32x16 and a texture of size 64x16 is to be added.
 * Currently, the stitcher takes the min of the texture's width and height, 16, and accounts for expanding to at least (32+16)x(16+16). To do
 * so, it will grow in height to a size of 32x32 by adding a slot of size 32x16. The stitcher then, without any further checks, adds the
 * texture to the new slot. As the texture does not fit into the slot, it is not added and the slot simply returns {@code false}. As the stitcher
 * ignores what the slot returns, the texture is never actually added.
 * <p>
 * This mixin makes the stitcher repeat the atlas expansion if the texture could not be added to the created slot.
 * <p>
 * Created 15/10/2024 by SuperMartijn642
 */
@Mixin(Stitcher.class)
public class StitcherMixin {

    @Final
    @Shadow
    private List<Stitcher.Slot> stitchSlots;

    @Unique
    private final List<Stitcher.Slot> dummyList = new ArrayList<>(1);

    @Shadow
    private boolean expandAndAllocateSlot(Stitcher.Holder texture){
        return false;
    }

    @Inject(
        method = "expandAndAllocateSlot",
        at = @At(
            value = "INVOKE",
            target = "Ljava/util/List;add(Ljava/lang/Object;)Z",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void validateTextureFits(Stitcher.Holder texture, CallbackInfoReturnable<Boolean> ci){
        // Get the slot that was just added
        Stitcher.Slot slot = this.stitchSlots.get(this.stitchSlots.size() - 1);
        // If the slot is empty, the texture was not actually added, so repeat expanding the atlas
        slot.getAllStitchSlots(this.dummyList);
        if(this.dummyList.isEmpty()){
            boolean success = this.expandAndAllocateSlot(texture);
            ci.setReturnValue(success);
        }else
            this.dummyList.clear();
    }
}
