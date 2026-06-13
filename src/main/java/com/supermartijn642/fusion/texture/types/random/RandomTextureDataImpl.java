package com.supermartijn642.fusion.texture.types.random;

import com.supermartijn642.fusion.api.texture.RawTextureInstance;
import com.supermartijn642.fusion.api.texture.types.random.RandomTextureData;
import com.supermartijn642.fusion.texture.types.base.BaseTextureDataImpl;
import org.jetbrains.annotations.Nullable;

/**
 * Created 22/10/2024 by SuperMartijn642
 */
public class RandomTextureDataImpl extends BaseTextureDataImpl implements RandomTextureData {

    private final int rows, columns;
    private final RandomnessSource randomSource;
    private final Long seed;
    private final RawTextureInstance<?,?> subTexture;
    private final boolean perTileAnimation;

    public RandomTextureDataImpl(RenderType renderType, boolean emissive, QuadTinting tinting, int rows, int columns, RandomnessSource randomSource, Long seed, RawTextureInstance<?,?> subTexture, boolean perTileAnimation){
        super(renderType, emissive, tinting);
        this.rows = rows;
        this.columns = columns;
        this.randomSource = randomSource;
        this.seed = seed;
        this.subTexture = subTexture;
        this.perTileAnimation = perTileAnimation;
    }

    @Override
    public int getRows(){
        return this.rows;
    }

    @Override
    public int getColumns(){
        return this.columns;
    }

    @Override
    public RandomnessSource getRandomSource(){
        return this.randomSource;
    }

    @Override
    public @Nullable Long getSeed(){
        return this.seed;
    }

    @Override
    public @Nullable RawTextureInstance<?,?> subTexture(){
        return this.subTexture;
    }

    @Override
    public boolean perTileAnimation(){
        return this.perTileAnimation;
    }
}
