package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.data.ConnectingModelData;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelType;
import com.supermartijn642.fusion.model.types.vanilla.VanillaModelType;
import net.minecraft.client.renderer.model.BlockModel;
import net.minecraft.client.renderer.model.IUnbakedModel;

/**
 * Contains references to the default model types provided by Fusion.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
public class DefaultModelTypes {

    /**
     * Model type used for vanilla {@link BlockModel} instances.
     */
    public static final ModelType<BlockModel> VANILLA = new VanillaModelType();
    /**
     * Model type used for any unknown models added by other mods.
     */
    public static final ModelType<IUnbakedModel> UNKNOWN = new UnknownModelType();
    /**
     * Model type which allows for connecting textures.
     * @see DefaultTextureTypes#CONNECTING
     * @see ConnectingModelDataBuilder
     */
    public static final ModelType<ConnectingModelData> CONNECTING = new ConnectingModelType();
}
