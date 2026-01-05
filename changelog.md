### Fusion 1.2.11b
- Fixed `enchantment` item model modifier predicate not working for enchanted books

### Fusion 1.2.11a
- Fixed Fusion's `pack.mcmeta` data not getting loaded for mod resources

### Fusion 1.2.11
- Added `show_breaking_overlay` option to block model modifiers to not show the breaking overlay for appended models
- Fixed crash when modded model bakeries do not contain model modifiers' target models

### Fusion 1.2.10
- Fixed all Fusion models loaded after any Fusion model has an error being broken

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

### Fusion 1.2.7
- Initial release of Fusion for Minecraft 1.21.6 & 1.21.7
