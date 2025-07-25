### Fusion 1.2.9
- Fixed `pieced` layout when a quads' uv does not cover the entire sprite
- Fixed connecting textures using connections for the wrong direction for rotated quads with mirrored uv in some cases

### Fusion 1.2.8
- Added Hungarian translations (thanks to bayi!)
- Ambient occlusion is now disabled for emissive quads
- Fixed quads being emitted 7 times for `base` and `connecting` models when rendered as items
- Fixed quads with different render types being ordered randomly for `base` and `connecting` models when rendered as items
- Fixed inverted vertical tile ordering for `continuous` textures
- Fixed `DefaultConnectionPredicates#isFaceVisible` returning `is_same_block` predicate
- Fixed `NotConnectionPredicate` serialization being invalid
- Fixed render type hint from NeoForge's model format being ignored
- Fixed crash when evaluating `biome` and `dimension` entity predicate
- Fixed `random` texture seed always being 0 for bottom side
- Fixed overrides folder not working for resource packs which use vanilla resource overlays
- Fixed entity model modifiers using model for incorrect layer when targeting entities with multiple vanilla layers

### Fusion 1.2.7b
- Fixed integration with FramedBlocks

### Fusion 1.2.7a
- Moved connection evaluation for connecting models fixing Fusion caching issue when combined with FramedBlocks

### Fusion 1.2.7
- Fixed argument validation for `count` and `durability` item predicates

### Fusion 1.2.6a
- Fixed `connecting` and `base` models not always using correct render type when rendered as items

### Fusion 1.2.6
- Added data providers for block model modifiers and item model modifiers

### Fusion 1.2.5
- Fixed emissive quads not being quite as bright as they should be

### Fusion 1.2.4
- Fixed vertex permutations for rotated textures being wrong and hence `pieced` layout not looking correct when rotated
- Account for breaking changes in NeoForge 21.4.84-beta

### Fusion 1.2.3
- Added special casing for `builtin/generated` parent model to make it work properly
- Added Turkish translations (thanks to RuyaSavascisi!)

### Fusion 1.2.2
- Fixed `pane_culling_fix` culling being inverted, culling only quads which should not be
- Fixed crash when mods use non-resource characters in model layer names

### Fusion 1.2.1
- Fixed texture references overwriting model references in connecting models
- Fixed block model modifiers with only `pane_culling_fix` option getting ignored

### Fusion 1.2.0
- Added base model type
  - Allows for processing random and continuous textures
  - Base models can have multiple parent models
  - Connecting model type inherits all properties from the base model type
- Added base texture type
  - Allows specifying emissiveness, custom render type, and custom tinting function
  - Added `biome_grass`, `biome_foliage`, and `biome_water` tinting functions
  - All texture types inherit properties from base texture type
- Added block model modifiers
  - Allows overlaying additional models onto blocks
  - Added `pane_culling_fix` to cull the top/bottom quads of glass panes
- Added item model modifiers
  - Allows for conditionally changing item models
  - Added `count`, `durability`, `enchantment`, and `potion` item predicates
- Added custom entity models
- Added entity model modifiers
  - Allows for random or conditional entity models and textures
  - Added `altitude`, `is_baby`, `biome`, and `dimension` entity predicates
- Added a warning screen when a resource pack requires a newer Fusion version than the one installed
- Added `pieced` connecting texture layout allowing bordered textures from only a few tiles
- Added `overlay` connecting texture layout intended for block overlays
- All Fusion texture types can now be animated
- Added continuous texture type for textures which span multiple blocks
- Added random texture type for randomly choosing from a number of variations
- Connecting models now allow specifying a `connections` key similar to the `texture` key
- Added `match_block_in_front`, `match_state_in_front`, and `is_direction` connection predicates
- Improved error messages for `match_state` connection predicate
- Drastically improved performance of connecting models
- Added compatibility for Embeddium, Iris, ModernFix, Oculus, Rubidium, Sodium, and VintageFix
- Removed empty space from the `full` connecting texture layout
- Fixed `match_state` predicate not working correctly when not all block state properties are specified
- Fixed incorrect connections for connected textures when using UV lock or texture rotations
- Fixed specifying connections per texture in connecting models not working correctly
- Fixed connecting model sometimes using the wrong render type
- Fixed caching states in connecting model sometimes leading to incorrect connections

### Fusion 1.1.1
- Initial release of Fusion for Minecraft 1.21.4
