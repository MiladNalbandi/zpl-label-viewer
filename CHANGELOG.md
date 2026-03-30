# Changelog

## [1.1.0] — 2024-03-30

### Changed
- Extracted rendering engine to standalone [zpl-renderer](https://github.com/Milimarty/zpl-renderer) library
- Renamed plugin class from `MyToolWindowFactory` to `ZplToolWindowFactory`
- Updated plugin ID to `com.miladnalbandi.zpl-label-viewer`

### Added
- Auto-detect input type (ZPL vs Base-64) on paste
- Render source dropdown: Local / Local→API fallback / Labelary API
- Fit-to-window preview with zoom indicator (no scrollbars)
- Zoom level shown in status bar
- IntelliJ-themed styled buttons with visible borders

### Fixed
- `^CF` (Change Default Font) now applied correctly
- `^FR` / `^FI` treated as Field Reverse (not rotation)
- `^BY` barcode height reads from correct parameter index
- `^GB` color and rounding parameters in correct order
- `^A` font orientation extracted from fused first token
- Underscores in field data no longer replaced with spaces
- `^GFA` compressed bitmap decoder (nibble-based RLE) fully implemented

## [1.0.0] — initial release

- Initial ZPL decoder plugin with Labelary API support
