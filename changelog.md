### Fusion 1.1.0c
- Fixed Fusion's appearance API checks, so it works properly with mods like FramedBlocks

### Fusion 1.1.0b
- Added workaround for weird Fabric API quirks when caching quads in connecting models

### Fusion 1.1.0a
- Added ModMenu library badge
- Fixed concurrency issue when checking connections for connecting models
- Fixed crash with Indium

### Fusion 1.1.0
- Added an option for resource packs to have optional Fusion integration
- Added the option for connecting textures to override their render type
- Added the option to specify connections per texture in connecting models
- Added `horizontal`, `vertical`, and `compact` connecting texture layouts
- Added support for Fabric's appearance API
- Improved the performance of connecting models

### Fusion 1.0.6
- Fixed concurrency issue when rendering connecting models

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
- Fixed textures not loading correctly when ModernFix is installed
- Fixed `SpriteCreationContext` sometimes returning the wrong atlas size

### Fusion 1.0.0b
- Fusion is no longer marked as client-side only
- Fixed versioning

### Fusion 1.0.0a
- Fixed crash with Iris

### Fusion 1.0.0
- Initial release of Fusion
