package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.model.types.cuboid.AbstractCuboidModelDataBuilder;
import net.minecraft.core.Direction;

/**
 * Created 06/05/2026 by SuperMartijn642
 */
public abstract class AbstractBaseModelDataBuilder<T extends AbstractBaseModelDataBuilder<T,S>, S> extends AbstractCuboidModelDataBuilder<T,S> implements BaseModelData.Builder<T,S> {

    protected Direction shadeDirectionOverride;
    protected Boolean emissive;

    @Override
    public T shadeDirectionOverride(Direction direction){
        this.shadeDirectionOverride = direction;
        return this.self();
    }

    @Override
    public T emissive(Boolean emissive){
        this.emissive = emissive;
        return this.self();
    }

    private T self(){
        //noinspection unchecked
        return (T)this;
    }
}
