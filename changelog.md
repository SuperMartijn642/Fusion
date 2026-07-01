### Fusion 1.3.5
- Directional model predicates, i.e. `match_block` and `match_state`, are now rotated along with `composite` model entries
- Replaced vanilla default uv calculation of model elements with calculation that better handles elements outside the center block space
- Fixed deserialization of item transforms (`display` property) for Fusion models
- Fixed `match_block`, `match_state` model predicates and `match_block`, `match_state`, `match_block_in_front`, `match_state_in_front` connection predicates only using the last block from the `blocks` array property
- Fixed origin being offset for `composite` model transforms
- Fixed composition order of `composite` model transforms with blockstate file transforms
- Added workaround for Sodium breaking animated texture interpolation

### Fusion 1.3.4
- Fixed threading issue with `match_block` and `match_state` block predicates potentially leading to incorrect results or crashes
- Fixed element face 'uv' property being ignored

### Fusion 1.3.3
- Fixed deserialization of `dimension`, `match_block`, and `match_state` block predicates
- Fixed deserialization of `dimension` entity predicate

### Fusion 1.3.2a
- Fixed crash when mods bake models outside the normal baking window

### Fusion 1.3.2
- Fixed broken mixin crash
- Fixed "Reference map could not be read" warning
- Removed ModMenu 'library' tag

### Fusion 1.3.1
- Fixed `overlay` connecting texture layout producing way too many quads
- Fixed `connections` property of connecting textures not working
- Fixed base texture properties not working for `connecting` and `random` texture types
- Fixed geometry of `base` models being duplicated
- Fixed broken textures when ModernFix `mixin.perf.dynamic_resources` is enabled
- Fixed crash with quick pack mod
- Renamed `ConnectingModelData.ConnectingModelDataBuilder` to `ConnectingModelData.Builder`
- Fixed `CuboidModelDataBuilder#material` calling itself
- Fixed model materials map serialization not including '#' for references

### Fusion 1.3.0
- Model related changes:
  - Overhauled `ModelType` and how models are loaded:
    - Any model with a Fusion model as parent will now be loaded as a Fusion model
  - Changes for `base` model type:
    - Model can now be made emissive, through `emissive` property
    - Shading can now be disabled, through `shade` property
    - Ambient occlusion can now be disabled, through `ambient_occlusion` property
    - Lighting in guis can now be changed, through `gui_light` property
    - Elements can now have `light_emission`, `emissive`, `shade`, `ambient_occlusion` properties
    - Element faces can now have `light_emission`, `emissive`, `shade`, `ambient_occlusion` properties
  - Overhauled block and item model modifiers:
    - Added `priority` property
    - Modifiers are now applied in order of `priority`, then file name, rather than 'random'
    - Missing targets (like modded items/blocks) can now be ignored, through `ignore_missing_targets` property
    - Added `default_model_overwrites` to allow changes the default model when item/block model predicates are met
    - Added `append_models` to append models when item/block model predicates are met
    - For block model modifiers, `show_breaking_overlay` can now be set per model entry
  - Added block model predicates:
    - Simple `true`, `false`, `and`, `or`, `not` types
    - `match_block` checks whether the block at a certain offset matches certain blocks
    - `match_state` checks whether the block at a certain offset matches certain blocks and state properties
    - `biome` checks whether the block is in a biome
    - `dimension` checks whether the block is in a dimension
    - `altitude` checks whether the block's y-position is within a range
  - Added item model predicates:
    - Simple `true`, `false`, `and`, `or`, `not` types
    - `count` checks if a stack's count is within a range
    - `durability` checks if a stack's durability is within a range
    - `enchantment` checks whether an enchantment is present has a certain level
    - `match_name` checks whether an item's custom name matches a string or regex
    - `potion` checks whether the stack has a potion type
  - Added `composite` model type:
    - Allows combining multiple models
    - Models can be applied conditionally based on item and block predicates
    - Models can be translated, scaled, and rotated
- Texture related changes:
  - Overhauled `TextureType` and how textures are loaded:
    - Textures can now have multiple texture atlas sprites
    - Textures can now have sub-textures
    - Any model using a Fusion texture will now be loaded as a Fusion model
    - Added generic quad processing for custom texture types
  - Changes for `base` texture type:
    - No longer requires a `base` model
  - Changes for `connecting` texture type:
    - No longer requires a `connecting` model
    - Each tile is now its own sprite on the texture atlas
    - Empty tiles (completely transparent) are ignored and not stitched or processed
    - Tiles can now use a sub-texture type, through `sub_texture` property
    - Animated image layout can now be per-tile, through `per_tile_animation` property
    - Added an option for a default connection predicate, through `connections` property
    - Changes to connection predicates:
      - Added `true` and `false` types
      - Added `ignore_missing` options for `match_block`, `match_state`, `match_block_in_front`, `match_state_in_front`
      - Allow specifying multiple blocks for `match_block`, `match_state`, `match_block_in_front`, `match_state_in_front`
  - Changes for `random` texture type:
    - Each tile is now its own sprite on the texture atlas
    - Empty tiles (completely transparent) are ignored and not stitched or processed
    - Tiles can now use a sub-texture type, through `sub_texture` property
    - Added randomness options `position`, `position_facing`, `position_axis`, through `random_source` property
    - Animated image layout can now be per-tile, through `per_tile_animation` property
    - Increased maximum `rows` and `columns` from 10 to 100
  - Changes for `continuous` texture type:
    - Default tile is now its own sprite on the texture atlas
    - Increased maximum `rows` and `columns` from 10 to 32
  - Changes for `scrolling` texture type:
    - Added 'wrap' loop type to allow the frame to wrap around the image
- General changes:
  - Error messages are now printed in a user-friendly readable format
  - Improved clarity of error messages
- Bug fixes:
  - Fixed handling of `full` layout connecting textures with a legacy square image
  - Fixed inconsistent randomness for block model modifier models
  - Fixed geometry key for block model modifiers
  - Fixed handling of generated item models
  - Fixed concurrency issue when using `pane_culling_fix` potentially leading to corrupted quads
  - Fixed item transforms not getting serialized for base model data
  - Fixed incorrect scaling of origin for rotation of base model data elements
  - Fixed not all vanilla model properties being serializable through `cuboid` model type
  - Fixed continuous textures not working correctly when rotated
  - Fixed thin edges sometimes showing around connecting, continuous, and random textures
  - Fixed sometimes broken mipmaps for Fusion textures on Minecraft 1.12
  - Fixed integration with FramedBlocks on Minecraft 1.20.1 and 1.21.1
  - Fixed inconsistent ordering of model modifiers on older Minecraft versions
  - Fixed model ordering of appended models in block model modifiers not being maintained when some models are vanilla and some are not

### Fusion 1.2.12
- Fixed handling of connections key references in connecting models
- Fixed texture references added through base model data builder having an extra '#'

### Fusion 1.2.11b
- Fixed `enchantment` item model modifier predicate not working for enchanted books

### Fusion 1.2.11a
- Fixed crash when using a connecting texture and a continuous or random texture on the same model

### Fusion 1.2.11
- Added `show_breaking_overlay` option to block model modifiers to not show the breaking overlay for appended models
- Fixed crash when modded model bakeries do not contain model modifiers' target models
- Fixed quad tinting sometimes taking tinting function from the wrong texture

### Fusion 1.2.10
- Fixed all Fusion models loaded after any Fusion model has an error being broken

### Fusion 1.2.9
- Fixed `pieced` layout when a quads' uv does not cover the entire sprite
- Fixed connecting textures using connections for the wrong direction for rotated quads with mirrored uv in some cases

### Fusion 1.2.8
- Added Hungarian translations (thanks to bayi!)
- Ambient occlusion is now disabled for emissive quads
- Fixed inverted vertical tile ordering for `continuous` textures
- Fixed `DefaultConnectionPredicates#isFaceVisible` returning `is_same_block` predicate
- Fixed `NotConnectionPredicate` serialization being invalid
- Fixed crash when evaluating `biome` and `dimension` entity predicate
- Fixed `random` texture seed always being 0 for bottom side
- Fixed overrides folder not working for resource packs which use vanilla resource overlays
- Fixed entity model modifiers using model for incorrect layer when targeting entities with multiple vanilla layer

### Fusion 1.2.7
- Fixed argument validation for `count` and `durability` item predicates

### Fusion 1.2.6
- Added data providers for block model modifiers and item model modifiers

### Fusion 1.2.5
- Fixed emissive quads not being quite as bright as they should be

### Fusion 1.2.4
- Fixed vertex permutations for rotated textures being wrong and hence `pieced` layout not looking correct when rotated

### Fusion 1.2.3
- Added special casing for `builtin/generated` parent model to make it work properly
- Added Turkish translations (thanks to RuyaSavascisi!)

### Fusion 1.2.2a
- Fixed custom texture render types not working for items
- Fixed custom texture tinting not working properly for connecting textures when Sodium is installed

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
- Initial release of Fusion for Minecraft 1.20.5 & 1.20.6
