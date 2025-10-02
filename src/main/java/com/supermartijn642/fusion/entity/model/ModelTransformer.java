package com.supermartijn642.fusion.entity.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created 30/09/2024 by SuperMartijn642
 */
public class ModelTransformer {

    public static ModelPart flipX(ModelPart model){
        return transform(
            model,
            part -> {
                part.x *= -1;
                part.yRot *= -1;
                part.zRot *= -1;
                PartPose pose = part.initialPose;
                pose = PartPose.offsetAndRotation(-pose.x(), pose.y(), pose.z(), pose.xRot(), -pose.yRot(), -pose.zRot());
                part.initialPose = pose;
            },
            cube -> {
                float maxX = cube.maxX;
                cube.maxX = -cube.minX;
                cube.minX = -maxX;
            },
            polygon -> polygon.normal.x *= -1,
            vertex -> vertex.x *= -1
        );
    }

    public static ModelPart flipY(ModelPart model){
        return transform(
            model,
            part -> {
                part.y *= -1;
                part.xRot *= -1;
                part.zRot *= -1;
                PartPose pose = part.initialPose;
                pose = PartPose.offsetAndRotation(pose.x(), -pose.y(), pose.z(), -pose.xRot(), pose.yRot(), -pose.zRot());
                part.initialPose = pose;
            },
            cube -> {
                float maxY = cube.maxY;
                cube.maxY = -cube.minY;
                cube.minY = -maxY;
            },
            polygon -> polygon.normal.y *= -1,
            vertex -> vertex.y *= -1
        );
    }

    public static ModelPart flipZ(ModelPart model){
        return transform(
            model,
            part -> {
                part.z *= -1;
                part.xRot *= -1;
                part.yRot *= -1;
                PartPose pose = part.initialPose;
                pose = PartPose.offsetAndRotation(pose.x(), pose.y(), -pose.z(), -pose.xRot(), -pose.yRot(), pose.zRot());
                part.initialPose = pose;
            },
            cube -> {
                float maxZ = cube.maxZ;
                cube.maxZ = -cube.minZ;
                cube.minZ = -maxZ;
            },
            polygon -> polygon.normal.z *= -1,
            vertex -> vertex.z *= -1
        );
    }

    public static ModelPart translateX(ModelPart model, float translation){
        translation *= 16;
        ModelPart copy = transform(
            model,
            part -> {},
            cube -> {},
            polygon -> {},
            vertex -> {}
        );
        copy.z += translation;
        PartPose pose = copy.initialPose;
        pose = PartPose.offsetAndRotation(pose.x(), pose.y(), pose.z() + translation, pose.xRot(), pose.yRot(), pose.zRot());
        copy.initialPose = pose;
        return copy;
    }

    public static ModelPart translateY(ModelPart model, float translation){
        translation *= 16;
        ModelPart copy = transform(
            model,
            part -> {},
            cube -> {},
            polygon -> {},
            vertex -> {}
        );
        copy.y += translation;
        PartPose pose = copy.initialPose;
        pose = PartPose.offsetAndRotation(pose.x(), pose.y() + translation, pose.z(), pose.xRot(), pose.yRot(), pose.zRot());
        copy.initialPose = pose;
        return copy;
    }

    public static ModelPart translateZ(ModelPart model, float translation){
        translation *= 16;
        ModelPart copy = transform(
            model,
            part -> {},
            cube -> {},
            polygon -> {},
            vertex -> {}
        );
        copy.x += translation;
        PartPose pose = copy.initialPose;
        pose = PartPose.offsetAndRotation(pose.x() + translation, pose.y(), pose.z(), pose.xRot(), pose.yRot(), pose.zRot());
        copy.initialPose = pose;
        return copy;
    }

    private static ModelPart transform(ModelPart part, Consumer<ModelPart> partTransform, Consumer<ModelPart.Cube> cubeTransform, Consumer<MutablePolygon> polygonTransform, Consumer<MutableVertex> vertexTransform){
        ModelPart copy;
        if(part instanceof DummyModelPart){
            copy = new DummyModelPart(transform(((DummyModelPart)part).getDummyChild(), partTransform, cubeTransform, polygonTransform, vertexTransform));
        }else{
            ImmutableList.Builder<ModelPart.Cube> cubes = ImmutableList.builderWithExpectedSize(part.cubes.size());
            for(ModelPart.Cube cube : part.cubes)
                cubes.add(transform(cube, cubeTransform, polygonTransform, vertexTransform));
            ImmutableMap.Builder<String,ModelPart> children = ImmutableMap.builderWithExpectedSize(part.children.size());
            for(Map.Entry<String,ModelPart> entry : part.children.entrySet())
                children.put(entry.getKey(), transform(entry.getValue(), partTransform, cubeTransform, polygonTransform, vertexTransform));
            copy = new ModelPart(cubes.build(), children.build());
        }
        copy.x = part.x;
        copy.y = part.y;
        copy.z = part.z;
        copy.xRot = part.xRot;
        copy.yRot = part.yRot;
        copy.zRot = part.zRot;
        copy.xScale = part.xScale;
        copy.yScale = part.yScale;
        copy.zScale = part.zScale;
        copy.visible = part.visible;
        copy.skipDraw = part.skipDraw;
        copy.initialPose = part.initialPose;
        partTransform.accept(copy);
        return copy;
    }

    private static ModelPart.Cube transform(ModelPart.Cube cube, Consumer<ModelPart.Cube> cubeTransform, Consumer<MutablePolygon> polygonTransform, Consumer<MutableVertex> vertexTransform){
        ModelPart.Cube copy = new ModelPart.Cube(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, Set.of());
        copy.polygons = Arrays.stream(cube.polygons).map(p -> transform(p, polygonTransform, vertexTransform)).toArray(ModelPart.Polygon[]::new);
        copy.minX = cube.minX;
        copy.minY = cube.minY;
        copy.minZ = cube.minZ;
        copy.maxX = cube.maxX;
        copy.maxY = cube.maxY;
        copy.maxZ = cube.maxZ;
        return copy;
    }

    private static ModelPart.Polygon transform(ModelPart.Polygon polygon, Consumer<MutablePolygon> polygonTransform, Consumer<MutableVertex> vertexTransform){
        MutablePolygon mutable = new MutablePolygon(
            Arrays.stream(polygon.vertices()).map(v -> transform(v, vertexTransform)).toArray(ModelPart.Vertex[]::new),
            new Vector3f(polygon.normal())
        );
        polygonTransform.accept(mutable);
        return mutable.build();
    }

    private static ModelPart.Vertex transform(ModelPart.Vertex vertex, Consumer<MutableVertex> vertexTransform){
        MutableVertex mutable = new MutableVertex(vertex);
        vertexTransform.accept(mutable);
        return mutable.build();
    }

    private static class MutablePolygon {
        ModelPart.Vertex[] vertices;
        Vector3f normal;

        public MutablePolygon(ModelPart.Vertex[] vertices, Vector3f normal){
            this.vertices = vertices;
            this.normal = normal;
        }

        public ModelPart.Polygon build(){
            return new ModelPart.Polygon(this.vertices, this.normal);
        }
    }

    private static class MutableVertex {
        float x, y, z, u, v;

        public MutableVertex(ModelPart.Vertex vertex){
            this.x = vertex.x();
            this.y = vertex.y();
            this.z = vertex.z();
            this.u = vertex.u();
            this.v = vertex.v();
        }

        public ModelPart.Vertex build(){
            return new ModelPart.Vertex(this.x, this.y, this.z, this.u, this.v);
        }
    }
}
