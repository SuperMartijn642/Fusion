package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.quad.EmittableQuad;
import com.supermartijn642.fusion.api.texture.custom.QuadProcessor;
import com.supermartijn642.fusion.api.texture.custom.SpriteInstance;
import com.supermartijn642.fusion.api.util.PropertyStore;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HiddenFaceSkippingTest {

    @Test
    public void existingCustomProcessorsDoNotOptIn(){
        assertFalse(new CustomProcessor().canSkipHiddenFaces());
    }

    @Test
    public void permissionRequiresSafeProcessorsAndMatchingDataOwner(){
        Object owner = new Object();
        BaseBakedModel.RenderData safe = new BaseBakedModel.RenderData(true, null, null, owner, true);
        BaseBakedModel.RenderData unsafe = new BaseBakedModel.RenderData(true, null, null, owner, false);
        assertTrue(BaseBakedModel.RenderData.canSkipHiddenFaces(safe, owner));
        assertFalse(BaseBakedModel.RenderData.canSkipHiddenFaces(safe, new Object()));
        assertFalse(BaseBakedModel.RenderData.canSkipHiddenFaces(unsafe, owner));
    }

    @Test
    public void missingDataKeepsOriginalPathAndFailedConditionsRemainEmpty(){
        Object owner = new Object();
        assertFalse(BaseBakedModel.RenderData.canSkipHiddenFaces(null, owner));
        assertTrue(BaseBakedModel.RenderData.canSkipHiddenFaces(BaseBakedModel.RenderData.FAILED_CONDITIONS, owner));
    }

    private static class CustomProcessor implements QuadProcessor<Object> {
        @Override public Object extractState(Supplier<RandomSource> random, PropertyStore properties){ return new Object(); }
        @Override public Object extractState(BlockAndTintGetter level, BlockPos pos, BlockState state, Supplier<RandomSource> random, PropertyStore properties){ return new Object(); }
        @Override public Object extractState(ItemStack stack, Supplier<RandomSource> random, PropertyStore properties){ return new Object(); }
        @Override public Object createGeometryKey(Object state, PropertyStore properties){ return null; }
        @Override public void processQuad(EmittableQuad quad, SpriteInstance sprite, Object state, PropertyStore properties){ throw new AssertionError("Eligibility must not process geometry"); }
    }
}
