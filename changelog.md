### Fusion 1.1.1
- Added `match_state` connection predicate
- Added `is_face_visible` connection predicate
- Fixed log spam for resource packs which don't have a pack.mcmeta file

### Fusion 1.1.0
- Added an option for resource packs to have optional Fusion integration
- Added the option for connecting textures to override their render type
- Added the option to specify connections per texture in connecting models
- Added `horizontal`, `vertical`, and `compact` connecting texture layouts
- Fixed connecting models not working when nested inside `WeightedBakedModel`

### Fusion 1.0.6
- Fixed concurrency issue when rendering connecting models

### Fusion 1.0.5b
- Fixed crash with VintageFix 0.3+

### Fusion 1.0.5a
- Added separate mixins for when VintageFix is installed

### Fusion 1.0.5
- Fixed `VanillaModelDataBuilder` setting ambient occlusion to false by default

### Fusion 1.0.4
- Fixed concurrency issue where some texture are sometimes not loaded correctly

### Fusion 1.0.3
- Added `ModelBakingContext#getModel`
- Fixed parents for connecting models not always being resolved

### Fusion 1.0.2
- Fixed simple connected texture sprite coordinates for the left,up,down connection

### Fusion 1.0.1
- Fixed `SpriteCreationContext` sometimes returning the wrong atlas size

### Fusion 1.0.0
- Initial release of Fusion
