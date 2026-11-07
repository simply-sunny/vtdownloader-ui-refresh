# Mod documentation website

Edit site.json for the name, description, and repository URL. Edit the HTML fragments in content/. Run `python3 build.py` to render all pages and the search index into public/. Articles and navigation work without JavaScript; search enhances them when JavaScript is available.

Run `python3 dev/preview.py --root public --port 8000` for local layout inspection. Keep dev/ outside published assets. Check with `python3 dev/check-production.py public`.

For GitHub Pages, choose GitHub Actions as the Pages source. The workflow builds pages, checks production assets, and uploads public/. Relative page and asset links work under repository subpaths.

Shared design source lives in templates/_shared in the templates repository. Generated projects contain independent copies in public/assets/base.css and fonts/. Fonts are self-hosted IBM Plex Latin; licenses are included. Add additional subsets if your content needs other scripts.

MIT, see LICENSE.

README refresh preserves pages with demo mounts, data-wiki-preserve annotations, or changes since the last import. It reports each preserved page; update those pages manually from the source. .synced-content.json tracks imported content; .generated-pages.json tracks build-owned HTML so removing a source page also removes its old output.
