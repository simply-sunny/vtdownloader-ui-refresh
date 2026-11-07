# CurseForge release sheet

CurseForge projects can't be created through the API, so the project is edited by hand at [authors.curseforge.com](https://authors.curseforge.com/) with these settings. The listing body starts after the horizontal rule. It matches `MODRINTH.md` except that mod links point at CurseForge.

## Project settings

| Field | Value |
| --- | --- |
| Name | **VTDownloader UI Refresh** |
| Slug | `vtdownloader-ui-refresh` |
| Project ID | `1785920` |
| Class | Mods |
| Summary | A new interface for VTDownloader's download screen. |
| License | MIT |
| Logo | `src/main/resources/assets/vtd_ui_refresh/icon.png` |
| Source | GitHub, https://github.com/simply-sunny/vtdownloader-ui-refresh |
| Issues | https://github.com/simply-sunny/vtdownloader-ui-refresh/issues |
| Wiki | https://simply-sunny.github.io/vtdownloader-ui-refresh/ |

## Gallery

Use the images, titles and descriptions from the Gallery table in `MODRINTH.md`. CurseForge names uploads after a UUID, so set each title and description, and feature `list`.

## Files

1.1.0 uploaded 2026-11-06 as file `9486588`.

---

A new interface for [VTDownloader](https://www.curseforge.com/minecraft/mc-mods/vtdownloader)'s download screen, in Vanilla Tweaks orange on black.

VTDownloader still does the work. Remove this mod and its own screen comes back.

- Category sidebar, list and grid views
- Search across every category
- Pack pages with previews and conflicts
- Installed packs, by zip
- Resizable side panels
- Back, forward and keyboard shortcuts
- `/vtd` commands to make, edit and turn on packs from chat

## Installation

For Minecraft 26.2 on Fabric. Client-side only.

1. Install [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api).
2. Install [VTDownloader](https://www.curseforge.com/minecraft/mc-mods/vtdownloader) 2.6.0.
3. Put the jar in `mods`.

Open Options → Resource Packs → Download VT Resource Pack.

## More

Keys and troubleshooting are on the [wiki](https://simply-sunny.github.io/vtdownloader-ui-refresh/).

MIT, see the [LICENSE](https://github.com/simply-sunny/vtdownloader-ui-refresh/blob/main/LICENSE). The screen is based on VTDownloader by IotaBread, also MIT. Packs are by [Vanilla Tweaks](https://vanillatweaks.net/).
