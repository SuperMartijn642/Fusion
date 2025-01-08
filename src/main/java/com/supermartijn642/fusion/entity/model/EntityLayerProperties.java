package com.supermartijn642.fusion.entity.model;

import com.supermartijn642.fusion.api.util.Pair;
import com.supermartijn642.fusion.entity.VanillaModelLayerProperties;
import com.supermartijn642.fusion.entity.model.predicates.EntityModelPredicate;
import com.supermartijn642.fusion.util.Triple;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Created 23/09/2024 by SuperMartijn642
 */
public class EntityLayerProperties {

    public static final RandomSource RANDOM = RandomSource.create();

    private final ModelLayerLocation identifier;
    private final List<ModelOption> defaultModel;
    private final List<Pair<EntityModelPredicate,List<ModelOption>>> conditionals;

    public EntityLayerProperties(ModelLayerLocation identifier, List<ModelOption> defaultModel, List<Pair<EntityModelPredicate,List<ModelOption>>> conditionals){
        this.identifier = identifier;
        this.defaultModel = defaultModel;
        this.conditionals = conditionals;
    }

    public ModelLayerLocation identifier(){
        return this.identifier;
    }

    public Triple<ModelPart,ResourceLocation,Float> chooseModel(Entity entity){
        // Find the model to use
        List<ModelOption> options = this.defaultModel;
        for(Pair<EntityModelPredicate,List<ModelOption>> conditional : this.conditionals){
            if(conditional.left().test(entity)){
                options = conditional.right();
                break;
            }
        }
        // If there's only a single option, avoid interacting with the random
        if(options.size() == 1 && (options.get(0).textures == null || options.get(0).textures.size() == 1))
            return Triple.of(options.get(0).model, options.get(0).textures == null ? null : options.get(0).textures.get(0), options.get(0).scaling);

        // Pick a random option from the weighted list of options
        RANDOM.setSeed(this.seed(entity.getUUID()));
        double value = RANDOM.nextDouble();
        double sum = 0;
        ModelOption option = null;
        for(ModelOption o : options){
            sum += o.weight;
            if(sum >= value){
                option = o;
                break;
            }
        }
        if(option == null)
            throw new AssertionError("Weights should add up to 1, yet no model was found for value '" + value + "'!");
        // Pick a random texture from the chosen option
        ResourceLocation texture = option.textures == null ? null : option.textures.size() > 1 ? option.textures.get(RANDOM.nextInt(option.textures.size())) : option.textures.get(0);
        return Triple.of(option.model, texture, option.scaling);
    }

    private long seed(UUID uuid){
        return uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits() ^ this.identifier.getLayer().hashCode();
    }

    public void gatherModels(Consumer<ModelPart> output){
        for(ModelOption option : this.defaultModel)
            output.accept(option.model);
        for(Pair<EntityModelPredicate,List<ModelOption>> conditional : this.conditionals){
            for(ModelOption option : conditional.right()){
                output.accept(option.model);
            }
        }
    }

    public EntityLayerProperties transformed(VanillaModelLayerProperties properties){
        return new EntityLayerProperties(
            this.identifier,
            this.defaultModel.stream().map(o -> o.transformed(properties)).toList(),
            this.conditionals.stream().map(c -> c.mapRight(l -> l.stream().map(o -> o.transformed(properties)).toList())).toList()
        );
    }

    public static class ModelOption {
        private final ModelPart model;
        private final boolean isVanillaModel;
        private final List<ResourceLocation> textures;
        private final double weight;
        private final Float scaling;

        public ModelOption(ModelPart model, boolean isVanillaModel, List<ResourceLocation> textures, double weight, Float scaling){
            this.model = model;
            this.isVanillaModel = isVanillaModel;
            this.textures = textures;
            this.weight = weight;
            this.scaling = scaling;
        }

        public double weight(){
            return this.weight;
        }

        public ModelPart model(){
            return this.model;
        }

        public boolean isVanillaModel(){
            return this.isVanillaModel;
        }

        public List<ResourceLocation> textures(){
            return this.textures;
        }

        public Float scaling(){
            return this.scaling;
        }

        private ModelOption transformed(VanillaModelLayerProperties properties){
            if(this.isVanillaModel)
                return this;
            ModelPart model = this.model;
            if(properties.getOffsetX() != 0)
                model = ModelTransformer.translateX(model, properties.getOffsetX());
            if(properties.getOffsetY() != 0)
                model = ModelTransformer.translateY(model, properties.getOffsetY());
            if(properties.getOffsetZ() != 0)
                model = ModelTransformer.translateZ(model, properties.getOffsetZ());
            if(properties.shouldFlipX())
                model = ModelTransformer.flipX(model);
            if(properties.shouldFlipY())
                model = ModelTransformer.flipY(model);
            if(properties.shouldFlipZ())
                model = ModelTransformer.flipZ(model);
            return new ModelOption(
                model,
                false,
                this.textures,
                this.weight,
                this.scaling
            );
        }
    }
}
