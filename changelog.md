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

## Update to Minecraft 26.2

## Update to Minecraft 26.1

### Fusion 1.2.12a
- Fixed performance issues when resolving custom texture tinting with Sodium installed

### Fusion 1.2.12
- Fixed handling of connections key references in connecting models
- Fixed texture references added through base model data builder having an extra '#'

### Fusion 1.2.11b
- Fixed `enchantment` item model modifier predicate not working for enchanted books

### Fusion 1.2.11a
- Fixed crash with Sodium

### Fusion 1.2.11
- Initial release of Fusion for Minecraft 1.21.11
