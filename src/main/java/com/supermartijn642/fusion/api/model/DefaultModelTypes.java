package com.supermartijn642.fusion.api.model;

import com.supermartijn642.fusion.api.model.types.CuboidModelDataBuilder;
import com.supermartijn642.fusion.api.model.types.base.BaseModelData;
import com.supermartijn642.fusion.api.model.types.composite.CompositeModelData;
import com.supermartijn642.fusion.api.model.types.connecting.ConnectingModelData;
import com.supermartijn642.fusion.api.texture.DefaultTextureTypes;
import com.supermartijn642.fusion.model.types.UnknownModelType;
import com.supermartijn642.fusion.model.types.base.BaseModelType;
import com.supermartijn642.fusion.model.types.composite.CompositeModelType;
import com.supermartijn642.fusion.model.types.connecting.ConnectingModelType;
import com.supermartijn642.fusion.model.types.cuboid.CuboidModelType;
import com.supermartijn642.fusion.model.types.itemgenerator.ItemModelGeneratorModelType;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.resources.model.UnbakedModel;

/**
 * Contains references to the default model types provided by Fusion.
 * <p>
 * Created 29/04/2023 by SuperMartijn642
 */
public final class DefaultModelTypes {

    /**
     * Model type used for any unknown models added by other mods.
     */
    public static final ModelType<UnbakedModel> UNKNOWN = new UnknownModelType();
    /**
     * Model type used for vanilla {@link BlockModel} instances.
     * @see CuboidModelDataBuilder
     */
    public static final ModelType<BlockModel> CUBOID = new CuboidModelType();
    /**
     * Model type used for vanilla {@link ItemModelGenerator} instances.
     * These models generate 3d geometry from 2d item textures.
     */
    public static final ModelType<ItemModelGenerator> ITEM_MODEL_GENERATOR = new ItemModelGeneratorModelType();

    /**
     * Model type which extends the vanilla model with some common properties and also allows for processing base texture properties.
     * @see BaseModelData
     */
    public static final ModelType<BaseModelData> BASE = BaseModelType.create();
    /**
     * Model type which allows for connecting textures.
     * @see ConnectingModelData
     * @see DefaultTextureTypes#CONNECTING
     */
    public static final ModelType<ConnectingModelData> CONNECTING = new ConnectingModelType();
    /**
     * Model type that composes multiple models.
     * @see CompositeModelData
     */
    public static final ModelType<CompositeModelData> COMPOSITE = new CompositeModelType();
}
