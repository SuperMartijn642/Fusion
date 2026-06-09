package com.supermartijn642.fusion.model.types.base;

import com.supermartijn642.fusion.api.model.custom.geometry.CuboidModelGeometry;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;

/**
 * Created 06/09/2024 by SuperMartijn642
 */
public class BaseModelDataBuilderImpl extends AbstractBaseModelDataBuilder<BaseModelDataBuilderImpl,BaseModelData> {

    public static BaseModelData.Builder<?,BaseModelData> builder(){
        return new BaseModelDataBuilderImpl();
    }

    private BaseModelDataBuilderImpl(){
    }

    @Override
    public BaseModelData build(){
        return new BaseModelDataImpl(
            this.parent,
            this.materials,
            this.ambientOcclusion,
            this.shade,
            this.emissive,
            this.guiLight,
            this.elements == null ? null : CuboidModelGeometry.of(this.elements),
            this.itemTransforms,
            this.itemOverrides,
            this.forgeRenderTypeGroup
        );
    }
}
