# Changelog

All notable changes to VTDownloader UI Refresh are documented here. This project follows [Semantic Versioning](https://semver.org/).

## [Unreleased]

### Changed

- Everything the mod saves is now in `config/vtd_ui_refresh/`: `config.json` and `cache/`. The layout is JSON instead of properties. Files from older versions move there on launch.

### Added

- **Commands:** `/vtd` lists, makes and edits Vanilla Tweaks zips, and turns them on or off. `--reload` applies a change straight away.
- **Companion mod:** the refreshed download screen as its own mod, requiring VTDownloader 2.6.0 or newer. Takes over wherever VTDownloader opens its screen. Remove it to get the original back.
- **Installed:** a row at the bottom of the sidebar lists the packs in the Vanilla Tweaks zips in your resource pack folder. Chips at the top show one zip at a time.
- **Keyboard:** the arrow keys move over the packs, and Space or Enter selects one. Cmd/Ctrl+1 to 9 open a category. Ctrl+Tab and Ctrl+Shift+Tab move through the sidebar.
- **Pack pages:** the arrow on a pack opens its preview, animated GIFs included, its whole description in the colours Vanilla Tweaks gives it, and the packs it can't be used with. Previews are kept in `cache/vtd_ui_refresh/previews`.
- **Icon cache:** pack icons are kept in `cache/vtd_ui_refresh/icons`, so they download once per Vanilla Tweaks version.
- **Range selection:** shift-click selects or deselects every pack from the last one clicked, in the list or the grid.
- **Select all:** Cmd/Ctrl+A selects every pack in the list, or deselects them if they're all selected. Cmd/Ctrl+Shift+A deselects everything. Neither acts while a text field has focus.
- **Search:** Cmd/Ctrl+F searches names and descriptions across all categories.
- **Side panels:** drag either edge to resize, or to the window's edge to hide. An arrow brings a panel back.
- **Back and forward:** buttons, Cmd/Ctrl+[ and ], and the mouse's side buttons. Each returns to where the list was scrolled.
- **Grid view:** a choice of list or tiles, remembered between sessions.

### Changed

- **Subcategories:** rows after their category's packs, with a few of their icons, or boxes in the grid.
- **Descriptions:** a list row shows two lines, and a pack's page the rest. Packs have no tooltips. Category warnings show all of their text.
- **Popups:** the share and error popups draw above the list, with room around the text.
- **New look:** Vanilla Tweaks orange on black, a vertical category sidebar, and a cleaner reload icon.

### Changed

- **Icon:** the panels fill a rounded shape of their own, with no tile around them.

### Documentation

- **Release sheets:** `MODRINTH.md` and `CURSEFORGE.md` hold the settings, gallery and listing for each store.

## [1.0.0] - 2026-11-05

### Fixed

- **Selected packs:** deselecting the last pack of a subcategory no longer leaves its category heading behind, drawn over the next one.
- **Hex warning colours:** the new screen reads colours like `#242423`, which crash VTDownloader's own screen.
