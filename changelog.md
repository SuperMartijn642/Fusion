### Fusion 1.2.9a
- Fixed crash when using model modifiers with mods using Puzzles Lib installed

### Fusion 1.2.9
- Fixed `pieced` layout when a quads' uv does not cover the entire sprite
- Fixed connecting textures using connections for the wrong direction for rotated quads with mirrored uv in some cases

### Fusion 1.2.8
- Added Hungarian translations (thanks to bayi!)
- Ambient occlusion is now disabled for emissive quads
- Fixed quads with different render types being ordered randomly for `base` and `connecting` models when rendered as items
- Fixed inverted vertical tile ordering for `continuous` textures
- Fixed `DefaultConnectionPredicates#isFaceVisible` returning `is_same_block` predicate
- Fixed `NotConnectionPredicate` serialization being invalid
- Fixed `pane_culling_fix` discarding render type and ambient occlusion properties
- Fixed crash when evaluating `biome` and `dimension` entity predicate
- Fixed `random` texture seed always being 0 for bottom side
- Fixed overrides folder not working for resource packs which use vanilla resource overlays
- Fixed entity model modifiers using model for incorrect layer when targeting entities with multiple vanilla layers

### Fusion 1.2.7b
- Changed `#createGeometryKey` to provide a globally unique value by including the model itself

### Fusion 1.2.7a
- Implemented `#createGeometryKey` for base and connecting models, so they can be cached

### Fusion 1.2.7
- Fixed argument validation for `count` and `durability` item predicates

### Fusion 1.2.6a
- Fixed `pane_culling_fix` causing crashes
- Fixed crash when Iris is installed

### Fusion 1.2.6
- Initial release of Fusion for Minecraft 1.21.5
