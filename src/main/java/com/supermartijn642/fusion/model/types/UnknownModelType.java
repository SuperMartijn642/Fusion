package com.supermartijn642.fusion.model.types;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.api.model.custom.*;
import com.supermartijn642.fusion.api.model.custom.geometry.ModelGeometry;
import com.supermartijn642.fusion.api.model.custom.quad.MutableQuad;
import com.supermartijn642.fusion.api.util.Either;
import com.supermartijn642.fusion.api.util.Property;
import com.supermartijn642.fusion.api.util.PropertyGetter;
import com.supermartijn642.fusion.model.SimpleModelType;
import com.supermartijn642.fusion.model.custom.geometry.ModelGeometryImpl;
import com.supermartijn642.fusion.util.CullingHelper;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created 30/04/2023 by SuperMartijn642
 */
public class UnknownModelType<T extends UnbakedModel> extends SimpleModelType<T> {

    public static Map<String,Either<String,ModelMaterial>> convertTextureSlots(TextureSlots.Data textureSlots){
        if(textureSlots.values().isEmpty())
            return Map.of();
        ImmutableMap.Builder<String,Either<String,ModelMaterial>> builder = ImmutableMap.builderWithExpectedSize(textureSlots.values().size());
        for(Map.Entry<String,TextureSlots.SlotContents> entry : textureSlots.values().entrySet()){
            if(entry.getValue() instanceof TextureSlots.Reference(String key))
                builder.put(entry.getKey(), Either.left(key));
            else if(entry.getValue() instanceof TextureSlots.Value(Material material))
                builder.put(entry.getKey(), Either.right(ModelMaterial.of(material)));
        }
        return builder.build();
    }

    @Override
    public Collection<Identifier> getDependencies(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(parent);
    }

    @Override
    public List<Either<Identifier,UntypedModelInstance>> getParents(T data){
        Identifier parent = data.parent();
        return parent == null ? List.of() : List.of(Either.left(parent));
    }

    @Override
    public Boolean getAmbientOcclusion(T data){
        return data.ambientOcclusion();
    }

    @Override
    public UnbakedModel.GuiLight getGuiLight(T data){
        return data.guiLight();
    }

    @Override
    public ItemTransform getItemTransform(ItemDisplayContext type, T data){
        ItemTransforms transforms = data.transforms();
        if(transforms == null)
            return null;
        ItemTransform transform = transforms.getTransform(type);
        return transform == ItemTransform.NO_TRANSFORM ? null : transform;
    }

    @Override
    public Map<String,Either<String,ModelMaterial>> getMaterials(T data){
        return convertTextureSlots(data.textureSlots());
    }

    @Override
    public ModelGeometry getGeometry(T data){
        UnbakedGeometry geometry = data.geometry();
        return geometry == null ? null : ModelGeometry.of(geometry);
    }

    @Override
    public @Nullable Boolean getShade(T data){
        return null;
    }

    @Override
    public @Nullable Boolean getEmissive(T data){
        return null;
    }

    @Override
    public <X, C> Optional<X> getProperty(Property<X,C> property, C context, T data){
        return Optional.empty();
    }

    @Override
    protected @Nullable Identifier getParent(T data){
        return data.parent();
    }

    @Override
    protected void bakeGeometry(BlockStateModelBakingContext context, ModelStack modelStack, T data, ModelTransform transform, ModelGeometry.MaterialResolver materialResolver, ModelGeometry.QuadConsumer quadConsumer){
        // Create dummy texture slots instance
        TextureSlots textureSlots = ModelGeometryImpl.createTextureSlots(materialResolver);
        // Create dummy model baker
        SpriteGetter spriteGetter = new SpriteGetter() {
            @Override
            public TextureAtlasSprite get(Material material, ModelDebugName name){
                return context.getMaterial(ModelMaterial.of(material));
            }

            @Override
            public TextureAtlasSprite reportMissingReference(String reference, ModelDebugName name){
                return materialResolver.get(reference);
            }
        };
        ModelBaker modelBaker = new ModelBaker() {
            @Override
            public ResolvedModel getModel(Identifier location){
                return context.getModelBaker().getModel(location);
            }

            @Override
            public BlockModelPart missingBlockModelPart(){
                return context.getMissingBlockStateModelPart();
            }

            @Override
            public SpriteGetter sprites(){
                return spriteGetter;
            }

            @Override
            public PartCache parts(){
                return context.getModelBaker().parts();
            }

            @Override
            public <X> X compute(SharedOperationKey<X> key){
                return context.getModelBaker().compute(key);
            }
        };
        // Bake the geometry
        UnbakedGeometry geometry = data.geometry();
        QuadCollection quadCollection;
        try{
            quadCollection = geometry.bake(
                textureSlots,
                modelBaker,
                transform.toModelState(),
                context.getModelIdentifier()::toString
            );
        }catch(Exception e){
            throw new RuntimeException("Encountered an exception baking geometry of class '" + geometry.getClass().getName() + "'!", e);
        }
        // Emit the quads
        for(Direction cullDirection : CullingHelper.cullDirections()){
            for(BakedQuad quad : quadCollection.getQuads(cullDirection)){
                quadConsumer.consume(MutableQuad.create(quad), cullDirection, PropertyGetter.empty());
            }
        }
    }

    @Override
    public T deserialize(JsonObject json) throws JsonParseException{
        throw new UnsupportedOperationException("Cannot deserialize unknown model type!");
    }

    @Override
    public JsonObject serialize(T value){
        throw new UnsupportedOperationException("Cannot serialize unknown model type!");
    }
}
