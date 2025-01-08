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
- Added `match_state` connection predicate
- Added `is_face_visible` connection predicate
- Fixed log spam for resource packs which don't have a pack.mcmeta file

### Fusion 1.1.0c
- Fix Forge's `render_type` property not working for connecting models

### Fusion 1.1.0b
- Fixed Fusion's appearance API checks, so it works properly with mods like FramedBlocks

### Fusion 1.1.0a
- Fixed crash when rendering the breaking overlay for connecting models
- Fixed concurrency issue when checking connections for connecting models

### Fusion 1.1.0
- Added an option for resource packs to have optional Fusion integration
- Added the option for connecting textures to override their render type
- Added the option to specify connections per texture in connecting models
- Added `horizontal`, `vertical`, and `compact` connecting texture layouts
- Added support for Forge's appearance API
- Fixed connecting models not respecting Forge's `render_type` property
- Fixed crash with OptiFine

### Fusion 1.0.6
- Fixed concurrency issue when rendering connecting models

### Fusion 1.0.5
- Fixed `VanillaModelDataBuilder` setting ambient occlusion to false by default

### Fusion 1.0.4
- Fixed concurrency issue where some texture are sometimes not loaded correctly

### Fusion 1.0.3
- Added `ModelBakingContext#getModel`
- Fixed parents for connecting models not always being resolved
- Added bypass to load child model data for multipart models

### Fusion 1.0.2
- Fixed simple connected texture sprite coordinates for the left,up,down connection

### Fusion 1.0.1
- Fixed textures not loading correctly when ModernFix is installed
- Fixed `SpriteCreationContext` sometimes returning the wrong atlas size

### Fusion 1.0.0a
- Fixed crash with Iris

### Fusion 1.0.0
- Initial release of Fusion
