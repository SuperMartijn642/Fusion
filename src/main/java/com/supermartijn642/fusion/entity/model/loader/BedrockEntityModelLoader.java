package com.supermartijn642.fusion.entity.model.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.supermartijn642.fusion.FusionClient;
import com.supermartijn642.fusion.entity.model.DummyModelPart;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.core.Direction;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created 17/09/2024 by SuperMartijn642
 */
public class BedrockEntityModelLoader implements EntityModelLoader {
    @Override
    public ModelPart loadModel(JsonObject json){
        // Read the schema version
        if(!json.has("format_version"))
            throw new JsonParseException("Missing 'format_version'!");
        if(!json.get("format_version").isJsonPrimitive() || !json.getAsJsonPrimitive("format_version").isString())
            throw new JsonParseException("Property 'format_version' must be a string!");
        SchemaVersion version = SchemaVersion.fromName(json.get("format_version").getAsString());
        if(version == null)
            throw new JsonParseException("Unknown 'format_version': '" + json.get("format_version").getAsString() + "'!");
        if(version.ordinal() < SchemaVersion.V1_12_0.ordinal())
            throw new JsonParseException("Unsupported 'format_version': '" + version + "'!");
        if(version.ordinal() > SchemaVersion.V1_12_0.ordinal())
            FusionClient.LOGGER.warn("Found an entity model with schema version '{}'. In case the model does not load as expected, please report it as a bug to Fusion!", version);

        // Read the 'minecraft:geometry' array
        if(!json.has("minecraft:geometry"))
            throw new JsonParseException("Missing 'minecraft:geometry'!");
        if(!json.get("minecraft:geometry").isJsonArray())
            throw new JsonParseException("Property 'minecraft:geometry' must be an array!");
        JsonArray geometryArray = json.getAsJsonArray("minecraft:geometry");
        if(geometryArray.isEmpty())
            throw new JsonParseException("Array property 'minecraft:geometry' must not be empty!");

        // Only consider the first element in the array
        if(!geometryArray.get(0).isJsonObject())
            throw new JsonParseException("Array property 'minecraft:geometry' must only contain objects!");
        JsonObject geometryJson = geometryArray.get(0).getAsJsonObject();
        // Read the geometry
        Geometry geometry = readGeometry(version, geometryJson);
        return geometry.bake();
    }

    private static Geometry readGeometry(SchemaVersion version, JsonObject json){
        List<Bone> roots = new ArrayList<>();

        // Read texture width and height from 'definitions'
        if(!json.has("description") || !json.get("description").isJsonObject())
            throw new JsonParseException("Geometry object must have object property 'description'!");
        JsonObject description = json.getAsJsonObject("description");
        if(!description.has("texture_width") || !description.get("texture_width").isJsonPrimitive() || !description.getAsJsonPrimitive("texture_width").isNumber())
            throw new JsonParseException("Geometry description must have int property 'texture_width'!");
        if(!description.has("texture_height") || !description.get("texture_height").isJsonPrimitive() || !description.getAsJsonPrimitive("texture_height").isNumber())
            throw new JsonParseException("Geometry description must have int property 'texture_height'!");
        int textureWidth = description.get("texture_width").getAsInt();
        int textureHeight = description.get("texture_height").getAsInt();
        if(textureWidth < 0)
            throw new JsonParseException("Property 'texture_width' must be greater than zero!");
        if(textureHeight < 0)
            throw new JsonParseException("Property 'texture_height' must be greater than zero!");

        // Read the bones
        Map<String,Bone> bones = new HashMap<>();
        if(json.has("bones")){
            if(!json.get("bones").isJsonArray())
                throw new JsonParseException("Property 'bones' must be an array!");
            JsonArray bonesJson = json.getAsJsonArray("bones");
            for(JsonElement boneJson : bonesJson){
                if(!boneJson.isJsonObject())
                    throw new JsonParseException("Array property 'bones' must only contain objects!");
                Bone bone = readBone(version, boneJson.getAsJsonObject());
                bones.put(bone.name, bone);
            }
        }

        // Resolve parents
        for(Bone part : bones.values()){
            if(part.parent == null){
                roots.add(part);
                continue;
            }
            if(!bones.containsKey(part.parent))
                throw new JsonParseException("Missing parent '" + part.parent + "' for bone '" + part.name + "'!");
            Bone parent = bones.get(part.parent);
            parent.children.add(part);
        }
        return new Geometry(textureWidth, textureHeight, roots);
    }

    private static Bone readBone(SchemaVersion version, JsonObject json){
        // Name and parent
        if(!json.has("name") || !json.get("name").isJsonPrimitive() || !json.getAsJsonPrimitive("name").isString())
            throw new JsonParseException("Bone must have string property 'name'!");
        String name = json.get("name").getAsString();
        String parent = null;
        if(json.has("parent")){
            if(!json.get("parent").isJsonPrimitive() || !json.getAsJsonPrimitive("parent").isString())
                throw new JsonParseException("Bone property 'parent' must be a string!");
            parent = json.get("parent").getAsString();
        }

        // Transforms
        float[] pivot = new float[3];
        if(json.has("pivot")){
            if(!json.get("pivot").isJsonArray())
                throw new JsonParseException("Bone property 'pivot' must be an array!");
            JsonArray arr = json.getAsJsonArray("pivot");
            if(arr.size() != 3
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber()
                || !arr.get(2).isJsonPrimitive() || !arr.get(2).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Bone property 'pivot' must consist of 3 floats!");
            pivot = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
        float[] rotation = new float[3];
        if(json.has("rotation")){
            if(!json.get("rotation").isJsonArray())
                throw new JsonParseException("Bone property 'rotation' must be an array!");
            JsonArray arr = json.getAsJsonArray("rotation");
            if(arr.size() != 3
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber()
                || !arr.get(2).isJsonPrimitive() || !arr.get(2).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Bone property 'rotation' must consist of 3 floats!");
            rotation = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }

        // Cube properties, these are overwritten by the properties on the cube itself
        boolean mirror = false;
        if(json.has("mirror")){
            if(!json.get("mirror").isJsonPrimitive() || !json.getAsJsonPrimitive("mirror").isBoolean())
                throw new JsonParseException("Bone property 'mirror' must be a boolean!");
            mirror = json.get("mirror").getAsBoolean();
        }
        float inflate = 0;
        if(json.has("inflate")){
            if(!json.get("inflate").isJsonPrimitive() || !json.getAsJsonPrimitive("inflate").isNumber())
                throw new JsonParseException("Bone property 'inflate' must be a number!");
            inflate = json.get("inflate").getAsFloat();
        }

        // Cubes
        List<Cube> cubes = List.of();
        if(json.has("cubes")){
            if(!json.get("cubes").isJsonArray())
                throw new JsonParseException("Bone property 'cubes' must be an array!");
            JsonArray cubesJson = json.getAsJsonArray("cubes");
            // Read all the cubes
            cubes = new ArrayList<>(cubesJson.size());
            for(JsonElement element : cubesJson){
                if(!element.isJsonObject())
                    throw new JsonParseException("Bone property 'cubes' must only contain objects!");
                cubes.add(readCube(version, element.getAsJsonObject()));
            }
        }

        // Warn about unsupported features
        if(json.has("poly_mesh"))
            throw new JsonParseException("Bone property 'poly_mesh' is not supported!");
        if(json.has("texture_meshes"))
            throw new JsonParseException("Bone property 'texture_meshes' is not supported!");

        return new Bone(name, parent, pivot, rotation, mirror, inflate, cubes);
    }

    private static Cube readCube(SchemaVersion version, JsonObject json){
        // Origin
        float[] origin = new float[3];
        if(json.has("origin")){
            if(!json.get("origin").isJsonArray())
                throw new JsonParseException("Cube property 'origin' must be an array!");
            JsonArray arr = json.getAsJsonArray("origin");
            if(arr.size() != 3
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber()
                || !arr.get(2).isJsonPrimitive() || !arr.get(2).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Cube property 'origin' must consist of 3 floats!");
            origin = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
        // Size
        float[] size = new float[3];
        if(json.has("size")){
            if(!json.get("size").isJsonArray())
                throw new JsonParseException("Cube property 'size' must be an array!");
            JsonArray arr = json.getAsJsonArray("size");
            if(arr.size() != 3
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber()
                || !arr.get(2).isJsonPrimitive() || !arr.get(2).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Cube property 'size' must consist of 3 floats!");
            size = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
        // Rotation
        float[] rotation = new float[3];
        if(json.has("rotation")){
            if(!json.get("rotation").isJsonArray())
                throw new JsonParseException("Cube property 'rotation' must be an array!");
            JsonArray arr = json.getAsJsonArray("rotation");
            if(arr.size() != 3
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber()
                || !arr.get(2).isJsonPrimitive() || !arr.get(2).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Cube property 'rotation' must consist of 3 floats!");
            rotation = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
        // Pivot
        float[] pivot = new float[3];
        if(json.has("pivot")){
            if(!json.get("pivot").isJsonArray())
                throw new JsonParseException("Cube property 'pivot' must be an array!");
            JsonArray arr = json.getAsJsonArray("pivot");
            if(arr.size() != 3
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber()
                || !arr.get(2).isJsonPrimitive() || !arr.get(2).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Cube property 'pivot' must consist of 3 floats!");
            pivot = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
        }
        // Inflate
        Float inflate = null;
        if(json.has("inflate")){
            if(!json.get("inflate").isJsonPrimitive() || !json.getAsJsonPrimitive("inflate").isNumber())
                throw new JsonParseException("Cube property 'inflate' must be a number!");
            inflate = json.get("inflate").getAsFloat();
        }
        // Mirror
        Boolean mirror = null;
        if(json.has("mirror")){
            if(!json.get("mirror").isJsonPrimitive() || !json.getAsJsonPrimitive("mirror").isBoolean())
                throw new JsonParseException("Cube property 'mirror' must be a boolean!");
            mirror = json.get("mirror").getAsBoolean();
        }
        // UV
        Map<Direction,Face> faces = new EnumMap<>(Direction.class);
        if(json.has("uv")){
            if(json.get("uv").isJsonArray()){
                JsonArray arr = json.getAsJsonArray("uv");
                if(arr.size() != 2
                    || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                    || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber())
                    throw new JsonParseException("Cube property 'uv' must consist of 2 floats!");
                for(Direction side : Direction.values())
                    faces.put(side, new Face(new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()}, null, 0, false));
            }else if(json.get("uv").isJsonObject()){
                JsonObject uvJson = json.getAsJsonObject("uv");
                for(Direction side : Direction.values()){
                    if(!uvJson.has(side.getName()))
                        continue;
                    if(!uvJson.get(side.getName()).isJsonObject())
                        throw new JsonParseException("Cube uv for side '" + side.getName() + "' must be an object!");
                    faces.put(side, readFace(version, uvJson.getAsJsonObject(side.getName()), side));
                }
            }else
                throw new JsonParseException("Cube property 'uv' must either be an object or an array!");
        }
        return new Cube(origin, size, rotation, pivot, inflate, mirror, faces);
    }

    private static Face readFace(SchemaVersion version, JsonObject json, Direction side){
        // UV
        if(!json.has("uv") || !json.get("uv").isJsonArray())
            throw new JsonParseException("Cube uv for side '" + side.getName() + "' must have array property 'uv'!");
        JsonArray arr = json.getAsJsonArray("uv");
        if(arr.size() != 2
            || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
            || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber())
            throw new JsonParseException("Property 'uv' in cube uv for side '" + side.getName() + "' must consist of 2 floats!");
        float[] uv = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()};
        // UV size
        float[] size = null;
        if(json.has("uv_size")){
            if(!json.get("uv_size").isJsonArray())
                throw new JsonParseException("Property 'uv_size' in cube uv for side '" + side.getName() + "' must be an array!");
            arr = json.getAsJsonArray("uv_size");
            if(arr.size() != 2
                || !arr.get(0).isJsonPrimitive() || !arr.get(0).getAsJsonPrimitive().isNumber()
                || !arr.get(1).isJsonPrimitive() || !arr.get(1).getAsJsonPrimitive().isNumber())
                throw new JsonParseException("Property 'uv_size' in cube uv for side '" + side.getName() + "' must consist of 2 floats!");
            size = new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat()};
        }
        // UV rotation
        int rotation = 0;
        if(json.has("uv_rotation")){
            if(!json.get("uv_rotation").isJsonPrimitive() || !json.getAsJsonPrimitive("uv_rotation").isNumber())
                throw new JsonParseException("Property 'uv_rotation' in cube uv for side '" + side.getName() + "' must be an integer!");
            rotation = json.get("uv_rotation").getAsInt();
        }
        return new Face(uv, size, rotation, true);
    }

    private enum SchemaVersion {
        V1_8_0("1.8.0"),
        V1_12_0("1.12.0"),
        V1_14_0("1.14.0"),
        V1_16_0("1.16.0"),
        V1_19_30("1.19.30"),
        V1_21_0("1.21.0");

        public static SchemaVersion fromName(String version){
            for(SchemaVersion value : values()){
                if(value.name.equals(version))
                    return value;
            }
            return null;
        }

        private final String name;

        SchemaVersion(String name){
            this.name = name;
        }
    }

    private static class Geometry {
        private final int textureWidth, textureHeight;
        private final List<Bone> bones;

        private Geometry(int textureWidth, int textureHeight, List<Bone> bones){
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.bones = bones;
        }

        public ModelPart bake(){
            Map<String,ModelPart> children = this.bones.stream()
                .collect(Collectors.toUnmodifiableMap(b -> b.name, b -> b.bake(this.textureWidth, this.textureHeight)));
            return new ModelPart(
                List.of(),
                children
            );
        }
    }

    private static class Bone {
        public final String name;
        public final String parent;
        private final float[] pivot;
        private final float[] rotation;
        private final boolean mirror;
        private final float inflate;
        private final List<Cube> cubes;
        public final List<Bone> children = new ArrayList<>();

        private Bone(String name, String parent, float[] pivot, float[] rotation, boolean mirror, float inflate, List<Cube> cubes){
            this.name = name;
            this.parent = parent;
            this.pivot = pivot;
            this.rotation = rotation;
            this.mirror = mirror;
            this.inflate = inflate;
            this.cubes = cubes;
        }

        public ModelPart bake(int textureWidth, int textureHeight){
            // Bake all the children
            Map<String,ModelPart> children = new HashMap<>();
            for(Bone child : this.children){
                children.put(
                    child.name,
                    child.bake(textureWidth, textureHeight)
                );
            }
            // Bake the cube and add their model parts to the children
            int index = 0;
            for(Cube cube : this.cubes){
                ModelPart part = cube.bake(this, textureWidth, textureHeight);
                // Find arbitrary unused key
                while(children.containsKey("Cube " + index))
                    index++;
                children.put(
                    "Cube " + index,
                    part
                );
            }
            // Create a model part with the children and this bone's transformations
            ModelPart mainPart = new ModelPart(
                List.of(),
                Map.copyOf(children)
            );
            PartPose pose = PartPose.offset(this.pivot[0], -this.pivot[1], -this.pivot[2]);
            mainPart.loadPose(pose);
            ModelPart pivotPart = new DummyModelPart(mainPart);
            pose = PartPose.offsetAndRotation(-this.pivot[0], this.pivot[1], this.pivot[2], (float)Math.toRadians(-this.rotation[0]), (float)Math.toRadians(-this.rotation[1]), (float)Math.toRadians(this.rotation[2]));
            pivotPart.loadPose(pose);
            return pivotPart;
        }
    }

    private static class Cube {
        private final float[] origin;
        private final float[] size;
        private final float[] rotation;
        private final float[] pivot;
        private final Float inflate;
        private final Boolean mirror;
        private final Map<Direction,Face> uvs;

        private Cube(float[] origin, float[] size, float[] rotation, float[] pivot, Float inflate, Boolean mirror, Map<Direction,Face> uvs){
            this.origin = origin;
            this.size = size;
            this.rotation = rotation;
            this.pivot = pivot;
            this.inflate = inflate;
            this.mirror = mirror;
            this.uvs = uvs;
        }

        public ModelPart bake(Bone bone, int textureWidth, int textureHeight){
            float inflate = this.inflate == null ? bone.inflate : this.inflate;
            boolean mirror = this.mirror == null ? bone.mirror : this.mirror;

            // Adjust uv for target texture size
            for(Direction side : Direction.values()){
                this.uvs.computeIfPresent(side, (s, f) -> new Face(
                    f.uv,
                    f.size,
                    f.rotation,
                    f.sideSpecific
                ));
            }
            // Create the polygons for all the faces
            ModelPart.Polygon[] polygons = createPolygons(
                -this.origin[0] - this.size[0] - this.pivot[0], this.origin[1] + this.pivot[1], this.origin[2] + this.pivot[2],
                this.size[0], this.size[1], this.size[2],
                inflate,
                textureWidth, textureHeight,
                this.uvs,
                mirror
            );
            // Create a new cube and overwrite the polygons
            ModelPart.Cube cube = new ModelPart.Cube(
                0, 0,
                -this.origin[0] - this.size[0] - this.pivot[0], this.origin[1] + this.pivot[1], this.origin[2] + this.pivot[2],
                this.size[0], this.size[1], this.size[2],
                inflate, inflate, inflate,
                mirror,
                1,
                1
            );
            cube.polygons = polygons;

            // Wrap the cube in a model part which has the actual transformations
            ModelPart part = new ModelPart(List.of(cube), Map.of());
            PartPose pose = PartPose.offsetAndRotation(this.pivot[0], -this.pivot[1], -this.pivot[2], (float)Math.toRadians(-this.rotation[0]), (float)Math.toRadians(-this.rotation[1]), (float)Math.toRadians(this.rotation[2]));
            part.loadPose(pose);
            return part;
        }

        private static ModelPart.Polygon[] createPolygons(float startX, float startY, float startZ, float sizeX, float sizeY, float sizeZ, float inflation, int textureWidth, int textureHeight, Map<Direction,Face> faces, boolean mirror){
            float endX = startX + sizeX + inflation;
            float endY = startY + sizeY + inflation;
            float endZ = startZ + sizeZ + inflation;
            startX -= inflation;
            startY -= inflation;
            startZ -= inflation;
            if(mirror){
                float f = endX;
                endX = startX;
                startX = f;
            }
            ModelPart.Vertex minXMinYMinZ = new ModelPart.Vertex(startX, startY, startZ, 0, 0);
            ModelPart.Vertex maxXMinYMinZ = new ModelPart.Vertex(endX, startY, startZ, 0, 8);
            ModelPart.Vertex maxXMaxYMinZ = new ModelPart.Vertex(endX, endY, startZ, 8, 8);
            ModelPart.Vertex minXMaxYMinZ = new ModelPart.Vertex(startX, endY, startZ, 8, 0);
            ModelPart.Vertex minXMinYMaxZ = new ModelPart.Vertex(startX, startY, endZ, 0, 0);
            ModelPart.Vertex maxXMinYMaxZ = new ModelPart.Vertex(endX, startY, endZ, 0, 8);
            ModelPart.Vertex maxXMaxYMaxZ = new ModelPart.Vertex(endX, endY, endZ, 8, 8);
            ModelPart.Vertex minXMaxYMaxZ = new ModelPart.Vertex(startX, endY, endZ, 8, 0);
            int polygonIndex = 0;
            ModelPart.Polygon[] polygons = new ModelPart.Polygon[faces.size()];
            if(faces.containsKey(Direction.DOWN)){
                polygons[polygonIndex++] = createPolygon(
                    minXMinYMinZ, maxXMinYMinZ, maxXMinYMaxZ, minXMinYMaxZ,
                    sizeX + sizeZ, sizeZ,
                    sizeX + sizeX + sizeZ, 0,
                    sizeX, sizeZ,
                    textureWidth, textureHeight,
                    mirror,
                    Direction.DOWN,
                    faces
                );
            }
            if(faces.containsKey(Direction.UP)){
                polygons[polygonIndex++] = createPolygon(
                    minXMaxYMaxZ, maxXMaxYMaxZ, maxXMaxYMinZ, minXMaxYMinZ,
                    sizeZ, 0,
                    sizeX + sizeZ, sizeZ,
                    sizeX, sizeZ,
                    textureWidth, textureHeight,
                    mirror,
                    Direction.UP,
                    faces
                );
            }
            if(faces.containsKey(Direction.WEST)){
                polygons[polygonIndex++] = createPolygon(
                    minXMaxYMaxZ, minXMaxYMinZ, minXMinYMinZ, minXMinYMaxZ,
                    sizeX + sizeZ, sizeZ,
                    sizeX + sizeZ + sizeZ, sizeY + sizeZ,
                    sizeZ, sizeY,
                    textureWidth, textureHeight,
                    mirror,
                    Direction.WEST,
                    faces
                );
            }
            if(faces.containsKey(Direction.NORTH)){
                polygons[polygonIndex++] = createPolygon(
                    minXMaxYMinZ, maxXMaxYMinZ, maxXMinYMinZ, minXMinYMinZ,
                    sizeZ, sizeZ,
                    sizeX + sizeZ, sizeY + sizeZ,
                    sizeX, sizeY,
                    textureWidth, textureHeight,
                    mirror,
                    Direction.NORTH,
                    faces
                );
            }
            if(faces.containsKey(Direction.EAST)){
                polygons[polygonIndex++] = createPolygon(
                    maxXMaxYMinZ, maxXMaxYMaxZ, maxXMinYMaxZ, maxXMinYMinZ,
                    0, sizeZ,
                    sizeZ, sizeY + sizeZ,
                    sizeZ, sizeY,
                    textureWidth, textureHeight,
                    mirror,
                    Direction.EAST,
                    faces
                );
            }
            if(faces.containsKey(Direction.SOUTH)){
                polygons[polygonIndex] = createPolygon(
                    maxXMaxYMaxZ, minXMaxYMaxZ, minXMinYMaxZ, maxXMinYMaxZ,
                    sizeX + sizeZ + sizeZ, sizeZ,
                    sizeX + sizeX + sizeZ + sizeZ, sizeY + sizeZ,
                    sizeX, sizeY,
                    textureWidth, textureHeight,
                    mirror,
                    Direction.SOUTH,
                    faces
                );
            }
            return polygons;
        }

        private static ModelPart.Polygon createPolygon(ModelPart.Vertex vertex0, ModelPart.Vertex vertex1, ModelPart.Vertex vertex2, ModelPart.Vertex vertex3, float u0, float v0, float u1, float v1, float width, float height, int textureWidth, int textureHeight, boolean mirror, Direction side, Map<Direction,Face> faces){
            Face face = faces.get(Direction.DOWN);
            if(face.sideSpecific){
                float[] uvSize = face.size == null ? new float[]{width, height} : face.size;
                u0 = face.uv[0];
                u1 = face.uv[0] + uvSize[0];
                v0 = face.uv[1];
                v1 = face.uv[1] + uvSize[1];
            }else{
                u0 += face.uv[0];
                u1 += face.uv[0];
                v0 += face.uv[1];
                v1 += face.uv[1];
            }
            return new ModelPart.Polygon(new ModelPart.Vertex[]{vertex0, vertex1, vertex2, vertex3}, u0, v0, u1, v1, textureWidth, textureHeight, mirror, Direction.DOWN);
        }
    }

    private static class Face {
        private final float[] uv;
        private final float[] size;
        private final int rotation;
        private final boolean sideSpecific; // Whether the uv is for an entire cube or for this specific face

        private Face(float[] uv, float[] size, int rotation, boolean sideSpecific){
            this.uv = uv;
            this.size = size;
            this.rotation = rotation;
            this.sideSpecific = sideSpecific;
        }
    }
}
