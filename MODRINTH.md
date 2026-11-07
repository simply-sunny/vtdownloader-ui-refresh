# Modrinth release sheet

This file is the source of truth for the Modrinth project settings, version metadata, gallery, and listing body. The listing body starts after the horizontal rule.

## Project settings

| Field | Value |
| --- | --- |
| Title | **VTDownloader UI Refresh** |
| Slug | `vtdownloader-ui-refresh` |
| Project type | Mod |
| Project ID | `kfnKrQm1` |
| Summary | A new interface for VTDownloader's download screen. |
| License | MIT |
| Environment | `client_only` |
| Featured categories | `utility` |
| Source | https://github.com/simply-sunny/vtdownloader-ui-refresh |
| Issues | https://github.com/simply-sunny/vtdownloader-ui-refresh/issues |
| Wiki | https://simply-sunny.github.io/vtdownloader-ui-refresh/ |
| Icon | `src/main/resources/assets/vtd_ui_refresh/icon.png` |

## Version metadata

| JAR | Version ID |
| --- | --- |
| `vtdownloader-ui-refresh-26.2-1.0.0.jar` | `t04gSK9H` |

## Gallery

Upload in this order, with `list` featured.

| File | Title | Description |
| --- | --- | --- |
| `art/gallery/list.png` | Browse and select | Categories on the left, packs in the middle, and everything you've selected on the right, grouped by category. |
| `art/gallery/grid.png` | Grid view | Switch to tiles to see more packs at once. The choice is remembered. |
| `art/gallery/search.png` | Search every category | Cmd/Ctrl+F finds packs by name and description across all of Vanilla Tweaks. |
| `art/gallery/pack-page.png` | Pack pages | The arrow on a pack opens its preview, its whole description, and what it can't be used with. |
| `art/gallery/installed.png` | See what's installed | Installed lists the packs in the Vanilla Tweaks zips in your resource pack folder, with a chip for each zip. |

---

A new interface for [VTDownloader](https://modrinth.com/mod/vtdownloader)'s download screen, in Vanilla Tweaks orange on black.

VTDownloader still does the work. Remove this mod and its own screen comes back.

- Category sidebar, list and grid views
- Search across every category
- Pack pages with previews and conflicts
- Installed packs, by zip
- Resizable side panels
- Back, forward and keyboard shortcuts

## Installation

For Minecraft 26.2 on Fabric. Client-side only.

1. Install [Fabric API](https://modrinth.com/mod/fabric-api).
2. Install [VTDownloader](https://modrinth.com/mod/vtdownloader) 2.6.0.
3. Put the jar in `mods`.

Open Options → Resource Packs → Download VT Resource Pack.

## More

Keys and troubleshooting are on the [wiki](https://simply-sunny.github.io/vtdownloader-ui-refresh/).

MIT, see the [LICENSE](https://github.com/simply-sunny/vtdownloader-ui-refresh/blob/main/LICENSE). The screen is based on VTDownloader by IotaBread, also MIT. Packs are by [Vanilla Tweaks](https://vanillatweaks.net/).
