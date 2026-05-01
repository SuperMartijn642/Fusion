package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.data.BaseModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.model.types.base.BaseModelType;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelType;
import com.supermartijn642.fusion.model.types.vanilla.VanillaModelType;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;

/**
 * Contains references to the default model types provided by Fusion.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
public class DefaultModelTypes {

    /**
     * Model type used for vanilla {@link CuboidModel} instances.
     */
    public static final ModelType<CuboidModel> VANILLA = new VanillaModelType();
    /**
     * Model type used for any unknown models added by other mods.
     */
    public static final ModelType<UnbakedModel> UNKNOWN = new UnknownModelType<>();
    /**
     * Model type which extends the vanilla model with some common properties.
     */
    public static final ModelType<BaseModelData> BASE = new BaseModelType();
    /**
     * Model type which allows for connecting textures.
     * @see DefaultTextureTypes#CONNECTING
     * @see ConnectingModelDataBuilder
     */
    public static final ModelType<ConnectingModelData> CONNECTING = new ConnectingModelType();
}
