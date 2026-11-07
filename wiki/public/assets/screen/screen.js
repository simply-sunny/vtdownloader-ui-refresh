// The mod's download screen, drawn in GUI pixels on a canvas with the same numbers as VTDownloadScreen and its widgets.
// Nothing here downloads: it uses a snapshot of the pack list, and the icons come from vanillatweaks.net as in the game.

export const VT = 'https://vanillatweaks.net/assets/resources';

const BLACK = 0xFF000000, PANEL = 0xFF121212, ROW_HOVER = 0xFF2A2A2A, SELECTED = 0xFF232323, WHITE = 0xFFFFFFFF, DIM = 0xFFB3B3B3,
  FAINT = 0xFF6A6A6A, ACCENT = 0xFFE99743, ACCENT_HOVER = 0xFFF2B070, TRACK = 0xFF4D4D4D, CONTROL = 0xFF2A2A2A,
  CONTROL_HOVER = 0xFF3A3A3A, CONTROL_OFF = 0xFF1C1C1C, TINT = 0xFF1C1C1C;
const CONFLICT = [0xFF007EA7, 0xFF9B7BFF, 0xFF00FFC5, 0xFFA50021, 0xFF5A8DEE, 0xFF98CE00, 0xFF16E0BD, 0xFFE8E337, 0xFFDE0D92, 0xFFD14545];

const ARROW = 13, DESC_LINES = 2, SUB_ROW = 26;
const GAP = 4, BAR = 28, BTN = 18, MARGIN = 4, PAD = 8, ICON_BTN = 15, HEAD = 24, SIDE_HEAD = 32, ROW = 44, TILE_ROW = 60, CAT_ROW = 16, SEL_ROW = 14;
const DRAG_MIN = 80, HIDE_AT = 40, CENTER_MIN = 160, TAB_M = 2, TAB_W = 9, TAB_H = 25, INSET = TAB_M * 2 + TAB_W, NARROW = 300, SEL_FIT = 400;

const ICONS = {
  reload: ['..XXXX..X', '.X....X.X', 'X......XX', 'X....XXXX', 'X........', 'X.......X', 'X.......X', '.X.....X.', '..XXXXX..'],
  search: ['..XXXX...', '.X....X..', 'X......X.', 'X......X.', 'X......X.', '.X....X..', '..XXXX...', '......XX.', '.......XX'],
  list: ['.........', 'X.XXXXXXX', '.........', '.........', 'X.XXXXXXX', '.........', '.........', 'X.XXXXXXX', '.........'],
  grid: ['XXXX.XXXX', 'XXXX.XXXX', 'XXXX.XXXX', 'XXXX.XXXX', '.........', 'XXXX.XXXX', 'XXXX.XXXX', 'XXXX.XXXX', 'XXXX.XXXX'],
  back: ['....X', '...X.', '..X..', '.X...', 'X....', '.X...', '..X..', '...X.', '....X'],
  forward: ['X....', '.X...', '..X..', '...X.', '....X', '...X.', '..X..', '.X...', 'X....'],
  right: ['X..', '.X.', '..X', '.X.', 'X..'],
  arrow: ['...X..', '....X.', 'XXXXXX', '....X.', '...X..'],
  left: ['..X', '.X.', 'X..', '.X.', '..X'],
};
const CURSOR = ['X..........', 'XX.........', 'XoX........', 'XooX.......', 'XoooX......', 'XooooX.....', 'XoooooX....', 'XooooooX...', 'XoooooooX..', 'XooooXXXXX.', 'XooXooX....', 'XoX.XooX...', 'XX..XooX...', 'X....XooX..', '.....XXX...'];

const colors = new Map();
function css(argb) {
  let c = colors.get(argb);
  if (!c) {
    c = `rgba(${argb >>> 16 & 255},${argb >>> 8 & 255},${argb & 255},${((argb >>> 24) / 255).toFixed(3)})`;
    colors.set(argb, c);
  }
  return c;
}
function mix(from, to, t) {
  const ch = s => Math.round((from >>> s & 255) + ((to >>> s & 255) - (from >>> s & 255)) * t);
  return (0xFF000000 | ch(16) << 16 | ch(8) << 8 | ch(0)) >>> 0;
}
function parseColor(c) {
  let m = /^#([0-9a-f]{6})$/i.exec(c);
  if (m) return (0xFF000000 | parseInt(m[1], 16)) >>> 0;
  m = /rgba\((\d+),\s*(\d+),\s*(\d+)/.exec(c);
  return m ? (0xFF000000 | +m[1] << 16 | +m[2] << 8 | +m[3]) >>> 0 : ACCENT;
}

// Monocraft at the game font's size: capitals 7 pixels tall.
const measure = document.createElement('canvas').getContext('2d');
let fontPx = 8, fontLoad = null;
function loadFont(url) {
  return fontLoad ??= new FontFace('Monocraft', `url(${url})`).load().then(face => {
    document.fonts.add(face);
    measure.font = '100px Monocraft';
    fontPx = 700 / (measure.measureText('H').actualBoundingBoxAscent || 70);
    measure.font = `${fontPx}px Monocraft`;
  });
}
const widths = new Map();
function tw(s) {
  let w = widths.get(s);
  if (w === undefined) {
    w = Math.round(measure.measureText(s).width);
    if (widths.size > 6000) widths.clear();
    widths.set(s, w);
  }
  return w;
}
function clip(s, w) {
  if (tw(s) <= w) return s;
  const room = Math.max(0, w - tw('...'));
  let lo = 0, hi = s.length;
  while (lo < hi) {
    const mid = (lo + hi + 1) >> 1;
    if (tw(s.slice(0, mid)) <= room) lo = mid; else hi = mid - 1;
  }
  return s.slice(0, lo).trimEnd() + '...';
}
function wrap(s, w) {
  const lines = [];
  for (const paragraph of s.split('\n')) {
    let line = '';
    for (let word of paragraph.split(' ')) {
      // A word wider than the line is broken, as the game's text wrapping does
      while (tw(word) > w && word.length > 1) {
        let cut = word.length - 1;
        while (cut > 1 && tw(word.slice(0, cut)) > w) cut--;
        if (line) { lines.push(line); line = ''; }
        lines.push(word.slice(0, cut)); word = word.slice(cut);
      }
      const next = line ? line + ' ' + word : word;
      if (tw(next) <= w || !line) line = next;
      else { lines.push(line); line = word; }
    }
    if (line) lines.push(line);
  }
  return lines;
}

const images = new Map();
function icon(id) {
  let e = images.get(id);
  if (!e) {
    e = { img: new Image(), ok: false };
    e.img.referrerPolicy = 'no-referrer';
    e.img.onload = () => { e.ok = true; dirtyAll(); };
    const url = `${VT}/icons/resourcepacks/26.2/${id}.png`;
    let tries = 0;
    // The icon host can drop a request when the page asks for many at once
    e.img.onerror = () => { if (++tries <= 3) setTimeout(() => { e.img.src = `${url}?retry=${tries}`; }, 500 * 2 ** tries); };
    e.img.src = url;
    images.set(id, e);
  }
  return e;
}
const previews = new Map();
/** The preview vanillatweaks.net shows for a pack. A GIF keeps playing as the canvas redraws it. */
function preview(p) {
  let e = previews.get(p.id);
  if (!e) {
    e = { img: new Image(), ok: false, failed: false };
    e.img.referrerPolicy = 'no-referrer';
    e.img.onload = () => { e.ok = true; dirtyAll(); };
    e.img.onerror = () => { e.failed = true; dirtyAll(); };
    e.img.src = `${VT}/previews/resourcepacks/26.2/${p.id}.${p.ext || 'png'}`;
    previews.set(p.id, e);
  }
  return e;
}
/** Wraps coloured runs of text into lines of [text, colour] pieces. */
function wrapRich(runs, w) {
  const words = [];
  for (const [t, c] of runs) for (const part of t.split(/( )/)) if (part) words.push([part, c || null]);
  const lines = [];
  let line = [], width = 0;
  for (const [t, c] of words) {
    if (t === ' ') { if (line.length) { line.push([t, c]); width += tw(t); } continue; }
    if (width + tw(t) > w && line.length) {
      while (line.length && line.at(-1)[0] === ' ') line.pop();
      lines.push(line); line = []; width = 0;
    }
    line.push([t, c]); width += tw(t);
  }
  if (line.length) lines.push(line);
  return lines;
}
const live = new Set();
function dirtyAll() { for (const s of live) s.dirty = true; }

let dataLoad = null;
function loadData(base) {
  return dataLoad ??= fetch(base + 'data.json').then(r => r.json()).then(d => {
    const packs = new Map();
    const walk = (c, parent) => {
      c.parent = parent;
      for (const p of c.packs) { p.cat = c; packs.set(p.id, p); }
      for (const s of c.sub || []) walk(s, c);
    };
    d.categories.forEach(c => walk(c, null));
    const tidyWarning = c => { if (c.warning) c.warning.text = tidy(c.warning.text); (c.sub || []).forEach(tidyWarning); };
    d.categories.forEach(tidyWarning);
    // A stand-in for the packs a player already has: a few from the first categories
    d.installed = { name: 'Installed', packs: [], installed: true };
    // Stand-ins for the zips a player already has
    const pick = (from, n) => d.categories.slice(from, from + 4).flatMap(c => c.packs.slice(0, n).map(p => p.id));
    d.installedSources = [
      { file: 'My survival pack.zip', ids: pick(4, 1) },
      { file: 'VanillaTweaks_r139880_MC26.2.x.zip', ids: pick(2, 1) },
      { file: 'VanillaTweaks_r147040_MC26.2.x.zip', ids: pick(0, 2) },
    ];
    for (const p of packs.values()) {
      p.text = tidy(p.text);
      p.hay = (p.name + ' ' + p.text).toLowerCase();
      p.conflicts = new Set(p.not || []);
    }
    for (const p of packs.values()) for (const id of p.conflicts) packs.get(id)?.conflicts.add(p.id);
    d.packs = packs;
    return d;
  });
}

class Screen {
  constructor(data, opts = {}) {
    this.data = data;
    this.dirty = true;
    this.w = 427; this.h = 240;
    this.mouse = { x: -1, y: -1 };
    this.hideCursor = true;
    this.onSelect = opts.onSelect;
    this.reset(opts);
  }

  reset(opts = {}) {
    this.category = this.data.categories[0];
    this.selection = new Map();
    this.anchor = null;
    this.installedFilter = null; this.chipScroll = 0;
    this.details = null;
    this.cursor = null; this.cursorShown = false;
    this.colorOf = new Map();
    this.layoutMode = opts.layout || 'LIST';
    this.searching = false; this.query = ''; this.name = ''; this.focus = null;
    this.sideHidden = !!opts.hideSide; this.selHidden = !!opts.hideSelected;
    this.sideW = opts.sideW ?? -1; this.selW = opts.selW ?? -1;
    this.overlay = null;
    this.backStack = []; this.forwardStack = [];
    this.scroll = 0; this.catScroll = 0; this.selScroll = 0;
    this.drag = null; this.progress = -1; this.done = 0; this.popup = null; this.flash = null;
    this.rows = null;
    this.resize(this.w, this.h);
    this.dirty = true;
  }

  get narrow() { return this.w < NARROW; }
  get selFits() { return this.w >= 400; }
  get selShown() { return this.narrow ? this.overlay === 'sel' : this.selFits && !this.selHidden; }
  get sideShown() { return this.narrow ? this.overlay === 'side' : !this.sideHidden; }
  get panelsBottom() { return this.h - BAR; }
  get centerShown() { return !this.overlay; }

  get left() {
    if (this.narrow) return this.overlay === 'side' ? this.w : INSET;
    return this.sideHidden ? INSET : GAP * 2 + this.sideW;
  }
  get right() {
    if (this.narrow) return this.overlay === 'sel' ? 0 : this.w - INSET;
    if (!this.selFits) return this.w - GAP;
    return this.w - (this.selHidden ? INSET : GAP * 2 + this.selW);
  }

  resize(w, h) {
    const widthChanged = w !== this.w;
    this.w = w; this.h = h;
    if (this.sideW < 0) this.sideW = Math.min(160, Math.max(112, w >> 2));
    if (this.selW < 0) this.selW = Math.min(150, Math.max(104, Math.floor(w / 5)));
    if (this.narrow) {
      if (this.overlay === 'side') this.sideW = w - INSET - GAP;
      if (this.overlay === 'sel') this.selW = w - INSET - GAP;
    } else {
      this.overlay = null;
      this.sideW = this.clampSide(this.sideW);
      this.selW = this.clampSel(this.selW);
    }
    if (widthChanged) this.rows = null;
    this.dirty = true;
  }

  clampSide(v) { return Math.min(Math.max(v, DRAG_MIN), Math.max(DRAG_MIN, this.right - CENTER_MIN - GAP * 2)); }
  clampSel(v) { return Math.min(Math.max(v, DRAG_MIN), Math.max(DRAG_MIN, this.w - this.left - CENTER_MIN - GAP * 2)); }

  // Rows of the pack list
  get rowWidth() { return this.right - this.left - PAD * 2; }
  get listTop() { return GAP + HEAD; }
  get listHeight() { return this.panelsBottom - this.listTop; }

  results() {
    const terms = this.query.trim().toLowerCase().split(/\s+/);
    const out = [];
    const walk = c => {
      for (const p of c.packs) if (terms.every(t => p.hay.includes(t))) out.push({ type: 'pack', pack: p, h: this.packH(p) });
      (c.sub || []).forEach(walk);
    };
    this.data.categories.forEach(walk);
    return out;
  }

  buildRows() {
    const searching = this.searching && this.query.trim();
    let entries;
    if (searching) entries = this.results();
    else {
      const c = this.category;
      entries = [];
      if (c.installed) {
        const sources = this.data.installedSources;
        if (sources.length) {
          entries.push({ type: 'chips', chips: this.chips(), h: 26 });
          const chosen = sources.find(z => z.file === this.installedFilter);
          for (const p of this.installedPacks(chosen ? chosen.ids : sources.flatMap(z => z.ids))) entries.push({ type: 'pack', pack: p, h: this.packH(p) });
        }
      } else {
        if (c.warning) entries.push({ type: 'warning', warning: c.warning, h: wrap(c.warning.text, this.rowWidth - 20).length * 9 + 16 });
        for (const p of c.packs) entries.push({ type: 'pack', pack: p, h: this.packH(p) });
        // Subcategories come last, as on vanillatweaks.net
        for (const sub of c.sub || []) entries.push({ type: 'sub', cat: sub, h: SUB_ROW });
      }
    }
    if (this.layoutMode === 'GRID') {
      const cols = Math.max(1, Math.floor((this.rowWidth + 4) / 76));
      const rows = [];
      let tiles = [], boxes = [];
      const flush = () => {
        if (tiles.length) { rows.push({ type: 'tiles', tiles, cols, h: TILE_ROW }); tiles = []; }
        if (boxes.length) { rows.push({ type: 'boxes', boxes, cols, h: TILE_ROW }); boxes = []; }
      };
      for (const e of entries) {
        if (e.type === 'pack') { if (boxes.length) flush(); tiles.push(e.pack); if (tiles.length === cols) flush(); }
        else if (e.type === 'sub') { if (tiles.length) flush(); boxes.push(e.cat); if (boxes.length === cols) flush(); }
        else { flush(); rows.push(e); }
      }
      flush();
      entries = rows;
    }
    let y = 0;
    for (const e of entries) { e.top = y; y += e.h; }
    this.contentH = y + 4;
    this.rows = entries;
  }

  get maxScroll() { return Math.max(0, this.contentH - this.listHeight); }
  /** The packs with these ids, in category order, each once. */
  installedPacks(ids) {
    const want = new Set(ids), out = [];
    const walk = k => { for (const p of k.packs) if (want.has(p.id) && !out.includes(p)) out.push(p); (k.sub || []).forEach(walk); };
    this.topCats.forEach(walk);
    return out;
  }
  /** One chip per zip, and All when there's more than one. "VanillaTweaks_" and ".zip" are dropped from labels. */
  chips() {
    const sources = this.data.installedSources, out = [];
    if (sources.length > 1) out.push({ file: null, label: 'All', count: this.installedPacks(sources.flatMap(z => z.ids)).length });
    for (const z of sources) out.push({ file: z.file, label: z.file.replace(/\.zip$/i, '').replace(/^VanillaTweaks_/, ''), count: this.installedPacks(z.ids).length });
    let x = 0;
    for (const c of out) { c.w = Math.min(150, tw(c.label + ' ' + c.count) + 12); c.x = x; x += c.w + 4; }
    out.width = x - 4;
    return out;
  }
  get chipMax() { const r = this.rows?.find(e => e.type === 'chips'); return r ? Math.max(0, r.chips.width - this.rowWidth) : 0; }
  chipAt(r, m) {
    const x = m.x - (this.left + PAD) + Math.round(this.chipScroll);
    return r.chips.find(c => x >= c.x && x < c.x + c.w) || null;
  }
  /** The name and the start of the description; the rest is on the pack's page. */
  packH() { return ROW; }
  get textRoom() { return this.rowWidth - 46 - 12 - ARROW; }
  /** The first lines of a description, the last cut short with an ellipsis when there's more. */
  shortText(p, w) {
    const lines = wrap(p.text, w);
    if (lines.length <= DESC_LINES) return lines;
    return [...lines.slice(0, DESC_LINES - 1), clip(lines.slice(DESC_LINES - 1).join(' '), w)];
  }

  // What's where
  buttons() {
    const b = [], y = GAP + MARGIN;
    if (this.sideShown) b.push({ id: 'reload', x: GAP + this.sideW - ICON_BTN - MARGIN, y, w: ICON_BTN, h: ICON_BTN, icon: 'reload', tip: 'Reload Resource Packs' });
    if (this.centerShown) {
      b.push({ id: 'back', x: this.left + MARGIN, y, w: ICON_BTN, h: ICON_BTN, icon: 'back', tip: 'Back', off: !this.searching && !this.details && !this.backStack.length });
      b.push({ id: 'forward', x: this.left + MARGIN + ICON_BTN + 2, y, w: ICON_BTN, h: ICON_BTN, icon: 'forward', tip: 'Forward', off: !this.forwardStack.length });
      const grid = this.right - MARGIN - ICON_BTN, list = grid - 2 - ICON_BTN, search = list - MARGIN - ICON_BTN;
      b.push({ id: 'search', x: search, y, w: ICON_BTN, h: ICON_BTN, icon: 'search', tip: `Search (${isMac ? 'Cmd' : 'Ctrl'}+F)`, primary: this.searching });
      b.push({ id: 'list', x: list, y, w: ICON_BTN, h: ICON_BTN, icon: 'list', tip: 'List view', primary: this.layoutMode === 'LIST' });
      b.push({ id: 'grid', x: grid, y, w: ICON_BTN, h: ICON_BTN, icon: 'grid', tip: 'Grid view', primary: this.layoutMode === 'GRID' });
    }
    const showY = GAP + ((this.panelsBottom - GAP - TAB_H) >> 1);
    if (this.narrow) {
      const left = this.overlay !== 'side', right = this.overlay !== 'sel';
      if (this.overlay === 'side') b.push({ id: 'close', x: this.w - TAB_M - TAB_W, y: showY, w: TAB_W, h: TAB_H, icon: 'left', tip: 'Hide categories' });
      else if (this.overlay === 'sel') b.push({ id: 'close', x: TAB_M, y: showY, w: TAB_W, h: TAB_H, icon: 'right', tip: 'Hide selected packs' });
      else if (left && right) {
        b.push({ id: 'showSide', x: TAB_M, y: showY, w: TAB_W, h: TAB_H, icon: 'right', tip: 'Show categories' });
        b.push({ id: 'showSel', x: this.w - TAB_M - TAB_W, y: showY, w: TAB_W, h: TAB_H, icon: 'left', tip: 'Show selected packs' });
      }
    } else {
      if (this.sideHidden) b.push({ id: 'showSide', x: TAB_M, y: showY, w: TAB_W, h: TAB_H, icon: 'right', tip: 'Show categories' });
      if (this.selFits && this.selHidden) b.push({ id: 'showSel', x: this.w - TAB_M - TAB_W, y: showY, w: TAB_W, h: TAB_H, icon: 'left', tip: 'Show selected packs' });
    }
    const barY = this.panelsBottom + ((BAR - BTN) >> 1), has = this.selection.size > 0;
    if (this.narrow) {
      b.push({ id: 'download', x: this.w - GAP - 76, y: barY, w: 76, h: BTN, label: this.doneLabel(), primary: true, off: !has });
    } else {
      const done = this.w - GAP - 60, dl = done - MARGIN - 100, share = dl - MARGIN - 60;
      b.push({ id: 'share', x: share, y: barY, w: 60, h: BTN, label: 'Share', off: !has });
      b.push({ id: 'download', x: dl, y: barY, w: 100, h: BTN, label: this.doneLabel(), primary: true, off: !has });
      b.push({ id: 'finish', x: done, y: barY, w: 60, h: BTN, label: 'Done' });
    }
    return b;
  }

  doneLabel() { return this.done > 0 ? 'Downloaded!' : 'Download'; }
  nameBox() {
    const barY = this.panelsBottom + ((BAR - BTN) >> 1);
    const end = this.narrow ? this.w - GAP - 76 - MARGIN : this.w - GAP - 60 - MARGIN - 100 - MARGIN - 60 - MARGIN;
    return { x: GAP, y: barY, w: end - GAP, h: BTN };
  }
  searchBox() {
    const x = this.left + MARGIN + ICON_BTN * 2 + 2 + MARGIN, grid = this.right - MARGIN - ICON_BTN, search = grid - 2 - ICON_BTN - MARGIN - ICON_BTN;
    return { x, y: GAP + MARGIN, w: search - MARGIN - x, h: ICON_BTN };
  }
  sideDivider() { return GAP + this.sideW; }
  selDivider() { return this.w - GAP * 2 - this.selW; }
  overDivider(l, m) { return m.x >= l - 1 && m.x < l + GAP + 1 && m.y >= GAP && m.y < this.panelsBottom; }
  overSide(m) { return !this.narrow && !this.sideHidden && this.overDivider(this.sideDivider(), m); }
  overSel(m) { return !this.narrow && this.selFits && !this.selHidden && this.overDivider(this.selDivider(), m); }

  get topCats() { return this.data.categories; }
  /** The sidebar's rows: one per category. Subcategories are in the list, after their category's packs. */
  sideRows() { return this.topCats.map(c => ({ cat: c, depth: 0 })); }
  catY(i) { return GAP + SIDE_HEAD - Math.round(this.catScroll) + i * CAT_ROW; }
  get catBottom() { return this.panelsBottom - CAT_ROW - 4; }
  get installedY() { return this.panelsBottom - CAT_ROW; }
  get catMax() { return Math.max(0, this.sideRows().length * CAT_ROW - (this.catBottom - GAP - SIDE_HEAD)); }

  rowAt(m) {
    if (!this.centerShown || m.x < this.left || m.x >= this.right || m.y < this.listTop || m.y >= this.panelsBottom) return null;
    if (!this.rows) this.buildRows();
    const y = m.y - this.listTop - 2 + Math.round(this.scroll);
    const x0 = this.left + PAD;
    if (m.x < x0 || m.x >= x0 + this.rowWidth) return null;
    for (const r of this.rows) {
      if (y >= r.top && y < r.top + r.h) {
        if (r.type === 'pack') {
          const ax = x0 + this.rowWidth - 6 - ARROW, ay = this.listTop + 2 - Math.round(this.scroll) + r.top + ((r.h - 1 - ARROW) >> 1);
          return { row: r, arrow: m.x >= ax && m.x < ax + ARROW && m.y >= ay && m.y < ay + ARROW };
        }
        if (r.type !== 'tiles' && r.type !== 'boxes') return { row: r };
        if (y >= r.top + TILE_ROW - 4) return null;
        const tw2 = Math.floor((this.rowWidth - 4 * (r.cols - 1)) / r.cols);
        const i = Math.floor((m.x - x0) / (tw2 + 4));
        const items = r.tiles || r.boxes;
        if (i < items.length && m.x - x0 - i * (tw2 + 4) < tw2) {
          const ax = x0 + i * (tw2 + 4) + tw2 - 3 - ARROW, ay = this.listTop + 2 - Math.round(this.scroll) + r.top + 3;
          const arrow = m.x >= ax && m.x < ax + ARROW && m.y >= ay && m.y < ay + ARROW;
          return r.tiles ? { row: r, pack: items[i], i, arrow } : { row: r, box: items[i], i };
        }
        return null;
      }
    }
    return null;
  }

  // Selection
  isSelected(p) { return this.selection.has(p.id); }
  toggle(p) {
    if (this.selection.delete(p.id)) this.onSelect?.(p, false);
    else { this.selection.set(p.id, p); this.onSelect?.(p, true); }
    this.updateColors();
    this.dirty = true;
  }
  /** A click on a pack. With shift, every pack from the last one clicked takes on that pack's state. */
  click(p, shift) {
    this.cursor = p;
    if (shift && this.anchor) {
      const shown = this.shownPacks();
      const from = shown.indexOf(this.anchor), to = shown.indexOf(p);
      if (from !== -1 && to !== -1) {
        const on = this.isSelected(this.anchor);
        for (const q of shown.slice(Math.min(from, to), Math.max(from, to) + 1)) if (this.isSelected(q) !== on) this.toggle(q);
        return;
      }
    }
    this.toggle(p);
    this.anchor = p;
  }
  /** The packs in the list, in order, in either layout. */
  shownPacks() {
    if (!this.rows) this.buildRows();
    return this.rows.flatMap(r => r.type === 'pack' ? [r.pack] : r.type === 'tiles' ? r.tiles : []);
  }
  /** Selects every pack in the list, or deselects them if they're all selected already. */
  selectAllShown() {
    if (this.details) return;
    const shown = this.shownPacks();
    const on = !shown.every(p => this.isSelected(p));
    for (const p of shown) if (this.isSelected(p) !== on) this.toggle(p);
    this.anchor = null;
  }
  /** Arrow keys: the first press shows the cursor; then it moves, by a row of tiles for up and down in the grid. */
  moveCursor(dx, dy) {
    // In the list, right opens the pack's page and left leaves it
    if (this.details) { if (dx < 0 && this.layoutMode === 'LIST') return this.closeDetails(); return false; }
    if (dx > 0 && this.layoutMode === 'LIST' && this.cursorShown && this.shownPacks().includes(this.cursor)) { this.openDetails(this.cursor); return true; }
    const shown = this.shownPacks();
    if (!shown.length) return false;
    let i = shown.indexOf(this.cursor);
    if (i < 0 || !this.cursorShown) {
      if (i < 0) { const top = this.rows.find(r => r.top + r.h > this.scroll && (r.type === 'pack' || r.type === 'tiles')); i = top ? shown.indexOf(top.pack || top.tiles[0]) : 0; }
    } else if (this.layoutMode === 'GRID') {
      const cols = this.rows.find(r => r.type === 'tiles')?.cols || 1;
      i = Math.min(Math.max(i + dx + dy * cols, 0), shown.length - 1);
    } else if (dy) i = Math.min(Math.max(i + dy, 0), shown.length - 1);
    this.cursor = shown[i]; this.cursorShown = true;
    const r = this.rows.find(e => e.pack === this.cursor || e.tiles?.includes(this.cursor));
    if (r) { if (r.top < this.scroll) this.scroll = r.top; else if (r.top + r.h > this.scroll + this.listHeight) this.scroll = r.top + r.h - this.listHeight + 4; }
    this.dirty = true;
    return true;
  }
  activateCursor(shift) {
    if (this.details) { this.toggle(this.details.pack); return true; }
    if (!this.cursorShown || !this.shownPacks().includes(this.cursor)) return false;
    this.click(this.cursor, shift);
    return true;
  }
  /** Ctrl+Tab: the next row of the sidebar, wrapping around, with Installed last. */
  adjacentCategory(step) {
    const rows = [...this.sideRows().map(r => r.cat), this.data.installed];
    const i = this.searching ? -1 : rows.indexOf(rootOf(this.category));
    const next = i < 0 ? (step > 0 ? 0 : rows.length - 1) : (i + step + rows.length) % rows.length;
    this.setCategory(rows[next]);
  }
  clearSelection() {
    for (const p of [...this.selection.values()]) this.toggle(p);
    this.anchor = null;
  }
  /** Picked packs that conflict get the same colour, one colour for each group of them. */
  updateColors() {
    const next = new Map(), seen = new Map();
    for (const p of this.selection.values()) {
      if (next.has(p.id)) continue;
      const others = [...p.conflicts].filter(id => this.selection.has(id));
      if (!others.length) continue;
      const c = CONFLICT[seen.size % CONFLICT.length];
      seen.set(p.id, c);
      next.set(p.id, c);
      for (const id of others) if (!next.has(id)) next.set(id, c);
    }
    this.colorOf = next;
  }
  accent(p) { return this.colorOf.get(p.id) ?? ACCENT; }

  selectedRows() {
    const groups = new Map();
    for (const p of this.selection.values()) { if (!groups.has(p.cat)) groups.set(p.cat, []); groups.get(p.cat).push(p); }
    const rows = [];
    for (const [cat, packs] of groups) {
      rows.push({ type: 'cat', cat, label: cat.name });
      for (const p of packs) rows.push({ type: 'pack', pack: p, label: p.name });
    }
    return rows;
  }
  selRowAt(m) {
    if (!this.selShown || m.y < this.listTop || m.y >= this.panelsBottom) return null;
    const x0 = this.narrow ? INSET : this.w - GAP - this.selW;
    if (m.x < x0 || m.x >= x0 + this.selW) return null;
    const i = Math.floor((m.y - this.listTop - 2 + Math.round(this.selScroll)) / SEL_ROW);
    return this.selectedRows()[i] || null;
  }

  // A pack's page
  openDetails(p) { this.details = { pack: p, scroll: 0, h: 0 }; this.cursorShown = false; this.dirty = true; }
  closeDetails() { if (this.details) { this.details = null; this.dirty = true; return true; } return false; }

  // Actions
  setCategory(c) {
    this.details = null;
    if (c !== this.category) {
      this.backStack.push({ category: this.category, scroll: this.scroll });
      this.forwardStack = [];
    }
    this.category = c; this.scroll = 0; this.rows = null;
    if (this.searching) { this.searching = false; this.query = ''; if (this.focus === 'search') this.focus = null; }
    if (this.narrow) this.overlay = null;
    this.dirty = true;
  }
  move(from, to) {
    const e = from.pop();
    if (!e) return;
    to.push({ category: this.category, scroll: this.scroll });
    this.category = e.category; this.rows = null; this.buildRows();
    this.scroll = Math.min(e.scroll, this.maxScroll);
    this.dirty = true;
  }
  goBack() { if (this.closeDetails()) return; if (this.searching) this.closeSearch(); else this.move(this.backStack, this.forwardStack); }
  goForward() { if (this.searching) this.closeSearch(); this.move(this.forwardStack, this.backStack); }
  openSearch() {
    this.details = null;
    this.searching = true; this.focus = 'search'; this.rows = null; this.dirty = true;
    if (this.narrow) this.overlay = null;
  }
  closeSearch() {
    this.searching = false; this.query = ''; this.scroll = 0; this.rows = null; this.dirty = true;
    if (this.focus === 'search') this.focus = null;
  }
  setQuery(q) { this.query = q; this.scroll = 0; this.rows = null; this.dirty = true; }
  setLayout(m) { if (this.layoutMode !== m) { this.layoutMode = m; this.scroll = 0; this.rows = null; this.dirty = true; } }
  press(id) {
    switch (id) {
      case 'back': this.goBack(); break;
      case 'forward': this.goForward(); break;
      case 'search': this.searching ? this.closeSearch() : this.openSearch(); break;
      case 'list': this.setLayout('LIST'); break;
      case 'grid': this.setLayout('GRID'); break;
      case 'showSide':
        if (this.narrow) { this.overlay = 'side'; this.sideW = this.w - INSET - GAP; } else { this.sideHidden = false; this.sideW = this.clampSide(this.sideW); }
        this.rows = null; break;
      case 'showSel':
        if (this.narrow) { this.overlay = 'sel'; this.selW = this.w - INSET - GAP; } else { this.selHidden = false; this.selW = this.clampSel(this.selW); }
        this.rows = null; break;
      case 'close': this.overlay = null; break;
      case 'download': this.progress = 0; break;
      case 'share': this.popup = { lines: wrap('In the game, Share gives you a vanillatweaks.net link to this selection.', 170), until: performance.now() + 3200 }; break;
      case 'reload': this.flash = { id, until: performance.now() + 240 }; this.rows = null; break;
      case 'finish': this.reset({ hideSide: this.narrow || this.sideHidden && false }); break;
    }
    this.dirty = true;
  }

  pointerDown(m, shift = false) {
    this.cursorShown = false;
    if (this.progress >= 0) return;
    this.popup = null;
    if (this.overSide(m)) { this.drag = { which: 'side', from: this.sideW }; this.dirty = true; return; }
    if (this.overSel(m)) { this.drag = { which: 'sel', from: this.selW }; this.dirty = true; return; }
    for (const b of this.buttons()) {
      if (m.x >= b.x && m.x < b.x + b.w && m.y >= b.y && m.y < b.y + b.h) {
        if (!b.off) this.press(b.id);
        return;
      }
    }
    if (this.searching && this.centerShown) {
      const s = this.searchBox();
      if (m.x >= s.x && m.x < s.x + s.w && m.y >= s.y && m.y < s.y + s.h) { this.focus = 'search'; this.dirty = true; return; }
    }
    const n = this.nameBox();
    if (m.x >= n.x && m.x < n.x + n.w && m.y >= n.y && m.y < n.y + n.h) { this.focus = 'name'; this.dirty = true; return; }
    this.focus = null; this.dirty = true;
    if (this.sideShown && m.x >= GAP && m.x < GAP + this.sideW && m.y >= GAP + SIDE_HEAD && m.y < this.panelsBottom) {
      let c = null;
      if (m.y >= this.installedY) c = this.data.installed;
      else if (m.y < this.catBottom) c = this.sideRows()[Math.floor((m.y - GAP - SIDE_HEAD + Math.round(this.catScroll)) / CAT_ROW)]?.cat;
      if (c && (c !== this.category || this.searching)) this.setCategory(c);
      return;
    }
    if (this.details && this.centerShown && m.x >= this.left && m.x < this.right && m.y >= this.listTop && m.y < this.panelsBottom) {
      const b = this.detailsButton();
      if (m.x >= b.x && m.x < b.x + b.w && m.y >= b.y && m.y < b.y + b.h) this.toggle(this.details.pack);
      return;
    }
    const hit = this.rowAt(m);
    if (hit) {
      if (hit.row.type === 'sub') { this.setCategory(hit.row.cat); return; }
      if (hit.box) { this.setCategory(hit.box); return; }
      if (hit.arrow) { this.openDetails(hit.pack || hit.row.pack); return; }
      if (hit.row.type === 'chips') {
        const chip = this.chipAt(hit.row, m);
        if (chip && chip.file !== this.installedFilter) { this.installedFilter = chip.file; this.rows = null; this.scroll = 0; }
        return;
      }
      if (hit.pack) this.click(hit.pack, shift);
      else if (hit.row.type === 'pack') this.click(hit.row.pack, shift);
      return;
    }
    const sel = this.selRowAt(m);
    if (sel?.type === 'pack') this.toggle(sel.pack);
  }

  pointerMove(m) {
    this.mouse = m;
    if (this.drag) {
      if (this.drag.which === 'side') {
        const v = Math.floor(m.x) - GAP;
        this.sideHidden = v < HIDE_AT;
        if (!this.sideHidden) this.sideW = this.clampSide(v);
      } else {
        const v = this.w - GAP - Math.floor(m.x);
        this.selHidden = v < HIDE_AT;
        if (!this.selHidden) this.selW = this.clampSel(v);
      }
      this.rows = null;
    }
    this.dirty = true;
  }
  pointerUp() {
    if (!this.drag) return;
    if (this.drag.which === 'side' && this.sideHidden) this.sideW = this.drag.from;
    if (this.drag.which === 'sel' && this.selHidden) this.selW = this.drag.from;
    this.drag = null; this.dirty = true;
  }
  wheel(m, dy, dx = 0, shift = false) {
    const step = dy * 0.5;
    if (this.sideShown && m.x >= GAP && m.x < GAP + this.sideW) this.catScroll = Math.min(this.catMax, Math.max(0, this.catScroll + step));
    else if (this.selShown && m.x >= (this.narrow ? INSET : this.w - GAP - this.selW)) this.selScroll = Math.max(0, Math.min(this.selScroll + step, Math.max(0, this.selectedRows().length * SEL_ROW + 4 - this.listHeight)));
    else if (this.details) this.details.scroll = Math.max(0, Math.min(this.details.scroll + step, Math.max(0, this.details.h - this.listHeight)));
    else {
      if (!this.rows) this.buildRows();
      // The wheel over the chips scrolls them sideways, when they don't all fit. So do shift and the wheel, or a
      // trackpad swipe, anywhere in the list.
      const hit = this.rowAt(m), sideways = Math.abs(dx) > Math.abs(dy) || shift;
      if (this.chipMax > 0 && hit && (sideways || hit.row.type === 'chips')) this.chipScroll = Math.min(this.chipMax, Math.max(0, this.chipScroll + (Math.abs(dx) > Math.abs(dy) ? dx : dy) * 0.5));
      else if (!dy) { /* a sideways scroll with nothing to move */ }
      else this.scroll = Math.min(this.maxScroll, Math.max(0, this.scroll + step));
    }
    this.dirty = true;
  }
  tick(now, dt) {
    if (this.progress >= 0) {
      this.progress += dt / 1500;
      if (this.progress >= 1) { this.progress = -1; this.done = 2200; }
      this.dirty = true;
    }
    if (this.done > 0) { this.done -= dt; this.dirty = true; if (this.done <= 0) this.done = 0; }
    if (this.popup && now > this.popup.until) { this.popup = null; this.dirty = true; }
    if (this.flash && now > this.flash.until) { this.flash = null; this.dirty = true; }
    if (this.details?.pack.ext === 'gif' && now - (this.gifAt || 0) > 50) { this.gifAt = now; this.dirty = true; }
    if (this.focus) { const on = Math.floor(now / 300) % 2 === 0; if (on !== this.caret) { this.caret = on; this.dirty = true; } }
  }

  // Drawing
  render(g) {
    if (!this.rows) this.buildRows();
    this.scroll = Math.min(this.scroll, this.maxScroll);
    const m = this.progress >= 0 ? { x: -1, y: -1 } : this.mouse;
    g.rect(0, 0, this.w, this.h, BLACK);
    const bottom = this.panelsBottom;
    if (this.sideShown) g.rect(GAP, GAP, GAP + this.sideW, bottom, PANEL);
    if (this.centerShown) g.rect(this.left, GAP, this.right, bottom, PANEL);
    if (this.selShown) { const x = this.narrow ? INSET : this.w - GAP - this.selW; g.rect(x, GAP, x + this.selW, bottom, PANEL); }
    let tip = null;
    if (this.centerShown) tip = this.renderList(g, m);
    this.renderCategories(g, m);
    this.renderSelected(g, m);
    this.renderHeaders(g);
    this.renderDividers(g, m);
    this.renderName(g, m);
    if (this.searching && this.centerShown) this.renderSearch(g, m);
    for (const b of this.buttons()) {
      const hover = m.x >= b.x && m.x < b.x + b.w && m.y >= b.y && m.y < b.y + b.h && !b.off && !this.drag;
      this.renderButton(g, b, hover);
      if (hover && b.tip) tip = [b.tip];
    }
    if (this.popup) this.renderPopup(g);
    if (this.progress >= 0) this.renderProgress(g);
    if (tip && !this.drag && m.x >= 0 && !this.quietTips) this.renderTooltip(g, tip, m);
  }

  renderButton(g, b, hover) {
    const flash = this.flash?.id === b.id;
    const bg = b.off ? CONTROL_OFF : b.primary ? (hover ? ACCENT_HOVER : ACCENT) : (hover || flash ? CONTROL_HOVER : CONTROL);
    const fg = b.off ? FAINT : b.primary ? BLACK : WHITE;
    g.box(b.x, b.y, b.x + b.w, b.y + b.h, bg);
    if (b.icon) g.pixels(ICONS[b.icon], b.x + ((b.w - ICONS[b.icon][0].length) >> 1), b.y + ((b.h - ICONS[b.icon].length) >> 1), fg);
    else { const s = clip(b.label, b.w - 6); g.text(s, b.x + ((b.w - tw(s)) >> 1), b.y + ((b.h - 8) >> 1), fg); }
  }

  renderList(g, m) {
    if (this.details) { this.renderDetails(g, m); return null; }
    const x0 = this.left + PAD, top = this.listTop, bottom = this.panelsBottom, w = this.rowWidth;
    let tip = null;
    const hit = this.drag ? null : this.rowAt(m);
    g.clip(this.left, top, this.right, bottom);
    const off = top + 2 - Math.round(this.scroll);
    for (const r of this.rows) {
      const y = off + r.top;
      if (y + r.h <= top || y >= bottom) continue;
      const hover = hit?.row === r;
      if (r.type === 'pack') this.renderRow(g, r.pack, x0, y, w, r.h, hover, hover && hit.arrow);
      else if (r.type === 'tiles') {
        const tw2 = Math.floor((w - 4 * (r.cols - 1)) / r.cols);
        r.tiles.forEach((p, i) => this.renderTile(g, p, x0 + i * (tw2 + 4), y, tw2, hover && hit.i === i, hover && hit.i === i && hit.arrow));
      } else if (r.type === 'boxes') {
        const tw2 = Math.floor((w - 4 * (r.cols - 1)) / r.cols);
        r.boxes.forEach((c, i) => this.renderBox(g, c, x0 + i * (tw2 + 4), y, tw2, hover && hit.i === i));
      } else if (r.type === 'sub') {
        this.renderSubRow(g, r.cat, x0, y, w, r.h, hover);
      } else if (r.type === 'warning') {
        // The colour marks the note without filling it, so the text stays easy to read
        const c = parseColor(r.warning.color);
        g.rect(x0, y + 2, x0 + w, y + r.h - 2, TINT);
        g.rect(x0, y + 2, x0 + 2, y + r.h - 2, c);
        wrap(r.warning.text, w - 20).forEach((l, i) => g.text(l, x0 + 10, y + 8 + i * 9, DIM));
      } else if (r.type === 'chips') {
        const ct = y + 4, sx = Math.round(this.chipScroll), over = hover ? this.chipAt(r, m) : null;
        g.clip(x0, Math.max(y, this.listTop), x0 + w, Math.min(y + r.h, bottom));
        for (const c of r.chips) {
          const cx = x0 + c.x - sx, chosen = c.file === this.installedFilter, count = ' ' + c.count;
          g.box(cx, ct, cx + c.w, ct + 16, chosen ? ACCENT : c === over ? CONTROL_HOVER : CONTROL);
          const label = clip(c.label, c.w - 12 - tw(count));
          g.text(label, cx + 6, ct + 4, chosen ? BLACK : WHITE);
          g.text(count, cx + 6 + tw(label), ct + 4, chosen ? BLACK : FAINT);
        }
        g.unclip();
        const max = Math.max(0, r.chips.width - w);
        if (max > 0) {
          const thumb = Math.max(16, Math.floor(w * w / r.chips.width)), tx = x0 + Math.floor((w - thumb) * sx / max);
          g.rect(x0, ct + 19, x0 + w, ct + 20, ROW_HOVER);
          g.rect(tx, ct + 19, tx + thumb, ct + 20, FAINT);
        }
      }
    }
    g.unclip();
    // A tile's name cut short is shown whole; the description is on the pack's page
    if (hit?.pack && !hit.arrow) {
      const tw2 = Math.floor((w - 4 * (hit.row.cols - 1)) / hit.row.cols);
      if (tw(hit.pack.name) > tw2 - 6) tip = [hit.pack.name];
    }
    if (!this.rows.length && (this.searching || this.category.installed)) {
      const s = this.searching ? 'No packs found' : 'No Vanilla Tweaks packs in your resource pack folder';
      g.text(s, ((this.left + this.right) >> 1) - (tw(s) >> 1), top + (this.listHeight >> 1), FAINT);
    }
    if (this.maxScroll > 0) {
      const h = this.listHeight, size = Math.max(32, Math.floor(h * h / this.contentH));
      const y = top + Math.floor((Math.round(this.scroll) * (h - size)) / this.maxScroll);
      g.rect(this.right - 3, y, this.right - 1, y + size, TRACK);
    }
    return tip;
  }

  tipWidth() { return Math.floor(this.w / 5) * 2; }

  renderRow(g, p, x, y, w, rowH, hover, overArrow) {
    const sel = this.isSelected(p), h = rowH - 1, accent = this.accent(p), plain = sel && !this.colorOf.has(p.id);
    if (sel) { g.rect(x, y, x + w, y + h, mix(TINT, accent, hover ? 0.3 : 0.2)); g.rect(x, y, x + 2, y + h, accent); }
    else if (hover) g.rect(x, y, x + w, y + h, ROW_HOVER);
    const ix = x + 6, iy = y + ((h - 32) >> 1);
    g.icon(p.id, ix, iy, 32);
    const tx = ix + 40, room = this.textRoom;
    const lines = this.shortText(p, room), total = 9 + 2 + lines.length * 9, ty = y + ((h - total) >> 1);
    g.text(clip(p.name, room), tx, ty, plain ? ACCENT : WHITE);
    lines.forEach((l, i) => g.text(l, tx, ty + 11 + i * 9, DIM));
    arrowButton(g, x + w - 6 - ARROW, y + ((h - ARROW) >> 1), hover, overArrow);
    if (this.cursorShown && this.cursor === p) outline(g, x, y, x + w, y + h);
  }

  renderTile(g, p, x, y, w, hover, overArrow) {
    const sel = this.isSelected(p), bottom = y + TILE_ROW - 4, accent = this.accent(p), plain = sel && !this.colorOf.has(p.id);
    if (sel) { g.rect(x, y, x + w, bottom, mix(TINT, accent, hover ? 0.3 : 0.2)); g.rect(x, y, x + 2, bottom, accent); }
    else if (hover) g.rect(x, y, x + w, bottom, ROW_HOVER);
    g.icon(p.id, x + ((w - 32) >> 1), y + 4, 32);
    if (this.cursorShown && this.cursor === p) outline(g, x, y, x + w, bottom);
    const s = clip(p.name, w - 6);
    g.text(s, x + ((w - tw(s)) >> 1), y + 40, plain ? ACCENT : WHITE);
    if (hover) arrowButton(g, x + w - 3 - ARROW, y + 3, true, overArrow);
  }

  /** A few of a subcategory's icons, side by side, so it shows what's inside. */
  subIcons(g, c, x, y, n) { c.packs.slice(0, n).forEach((p, i) => g.icon(p.id, x + i * 18, y, 16)); }

  renderSubRow(g, c, x, y, w, h, hover) {
    g.box(x, y + 2, x + w, y + h - 2, hover ? ROW_HOVER : TINT);
    const count = `${c.packs.length} packs`, ax = x + w - 6 - ARROW;
    let n = Math.min(3, c.packs.length);
    while (n > 1 && x + 6 + n * 18 - 2 + 8 + tw(c.name) + 6 + tw(count) + 6 > ax) n--;
    this.subIcons(g, c, x + 6, y + ((h - 16) >> 1), n);
    const tx = x + 6 + n * 18 - 2 + 8, ty = y + ((h - 8) >> 1);
    const name = clip(c.name, ax - 6 - tw(count) - 6 - tx);
    g.text(name, tx, ty, hover ? WHITE : DIM);
    g.text(count, tx + tw(name) + 6, ty, FAINT);
    arrowButton(g, ax, y + ((h - ARROW) >> 1), hover, false);
  }

  renderBox(g, c, x, y, w, hover) {
    g.box(x, y, x + w, y + TILE_ROW - 4, hover ? CONTROL_HOVER : CONTROL);
    const n = Math.max(1, Math.min(3, c.packs.length, Math.floor((w - 8) / 18)));
    this.subIcons(g, c, x + ((w - (n * 18 - 2)) >> 1), y + 10, n);
    const s = clip(c.name, w - 6), count = `${c.packs.length} packs`;
    g.text(s, x + ((w - tw(s)) >> 1), y + 30, hover ? WHITE : DIM);
    g.text(count, x + ((w - tw(count)) >> 1), y + 41, FAINT);
    if (hover) arrowButton(g, x + w - 3 - ARROW, y + 3, true, false);
  }

  detailsButton() {
    return { x: this.right - PAD - 64, y: this.listTop + 8 - Math.round(this.details.scroll) + 8, w: 64, h: 16 };
  }

  /** A pack's page: its preview, playing if it's a GIF, the whole description in its colours, and a select button. */
  renderDetails(g, m) {
    const d = this.details, p = d.pack, top = this.listTop, bottom = this.panelsBottom;
    const x = this.left + PAD, w = this.right - this.left - PAD * 2;
    d.scroll = Math.max(0, Math.min(d.scroll, Math.max(0, d.h - this.listHeight)));
    g.clip(this.left, top, this.right, bottom);
    const y0 = top + 8 - Math.round(d.scroll);
    let y = y0;
    const sel = this.isSelected(p), b = this.detailsButton();
    g.icon(p.id, x, y, 32);
    const room = b.x - 8 - (x + 40);
    g.text(clip(p.name, room), x + 40, y + 6, sel ? ACCENT : WHITE);
    const where = p.cat.parent ? `${p.cat.parent.name} / ${p.cat.name}` : p.cat.name;
    g.text(clip(where, room), x + 40, y + 18, FAINT);
    const over = m.x >= b.x && m.x < b.x + b.w && m.y >= b.y && m.y < b.y + b.h;
    g.box(b.x, b.y, b.x + b.w, b.y + b.h, sel ? (over ? CONTROL_HOVER : CONTROL) : (over ? ACCENT_HOVER : ACCENT));
    const label = sel ? 'Deselect' : 'Select';
    g.text(label, b.x + ((b.w - tw(label)) >> 1), b.y + 4, sel ? WHITE : BLACK);
    y += 40;
    const e = preview(p);
    if (e.ok) {
      const h = Math.round(e.img.naturalHeight * w / e.img.naturalWidth);
      g.ctx.imageSmoothingEnabled = true; g.ctx.drawImage(e.img, x, y, w, h); g.ctx.imageSmoothingEnabled = false;
      y += h;
    } else {
      const h = e.failed ? 24 : Math.round(w * 9 / 16), s = e.failed ? 'No preview' : 'Loading preview...';
      g.box(x, y, x + w, y + h, TINT);
      g.text(s, x + ((w - tw(s)) >> 1), y + ((h - 8) >> 1), FAINT);
      y += h;
    }
    y += 8;
    for (const line of wrapRich(p.rich || [[p.text]], w)) {
      let lx = x;
      for (const [t, c] of line) { g.text(t, lx, y, c ? parseColor(c) : DIM); lx += tw(t); }
      y += 10;
    }
    const not = [...p.conflicts].map(id => this.data.packs.get(id)?.name || id);
    if (not.length) {
      y += 8;
      g.text("Can't be used with", x, y, WHITE); y += 12;
      for (const l of wrap(not.join(', '), w)) { g.text(l, x, y, DIM); y += 10; }
    }
    d.h = y - y0 + 16;
    g.unclip();
    if (d.h > this.listHeight) {
      const h = this.listHeight, size = Math.max(32, Math.floor(h * h / d.h));
      const ty = top + Math.floor(d.scroll * (h - size) / (d.h - h));
      g.rect(this.right - 3, ty, this.right - 1, ty + size, TRACK);
    }
  }

  renderCategories(g, m) {
    if (!this.sideShown) return;
    const top = GAP + SIDE_HEAD, bottom = this.catBottom, x = GAP + 2, w = this.sideW - 4;
    const current = this.searching && this.query.trim() ? null : this.category;
    const open = current?.parent || null;
    const row = (c, y, depth, lo, hi) => {
      const sel = c === current, hover = !this.drag && m.x >= x && m.x < x + w && m.y >= Math.max(y, lo) && m.y < Math.min(y + CAT_ROW, hi);
      if (sel || hover) g.rect(x, y, x + w, y + CAT_ROW, sel ? SELECTED : ROW_HOVER);
      const left = 6 + depth * 10;
      g.text(clip(c.name, w - left - 6), x + left, y + 4, sel || c === open ? ACCENT : hover ? WHITE : DIM);
    };
    const rows = this.sideRows();
    g.clip(GAP, top, GAP + this.sideW, bottom);
    rows.forEach((r, i) => {
      const y = this.catY(i);
      if (y + CAT_ROW > top && y < bottom) row(r.cat, y, r.depth, top, bottom);
    });
    g.unclip();
    g.rect(x, bottom + 1, x + w, bottom + 2, ROW_HOVER);
    row(this.data.installed, this.installedY, 0, this.installedY, this.panelsBottom);
    if (this.catMax > 0) {
      const h = bottom - top, size = Math.max(16, Math.floor(h * h / (rows.length * CAT_ROW)));
      const y = top + Math.floor(this.catScroll * (h - size) / this.catMax);
      g.rect(GAP + this.sideW - 3, y, GAP + this.sideW - 1, y + size, TRACK);
    }
  }

  renderSelected(g, m) {
    if (!this.selShown) return;
    const x = this.narrow ? INSET : this.w - GAP - this.selW, left = x + PAD, right = x + this.selW - PAD;
    const count = String(this.selection.size), has = this.selection.size > 0;
    g.text(count, right - tw(count), GAP + 8, has ? ACCENT : FAINT);
    g.text(clip('Selected packs', right - tw(count) - left - MARGIN), left, GAP + 8, WHITE);
    if (!has) { g.text(clip('Nothing yet', right - left), left, GAP + HEAD, FAINT); return; }
    const top = this.listTop, bottom = this.panelsBottom, rx = x + 2, rw = this.selW - 4;
    const hit = this.drag ? null : this.selRowAt(m);
    g.clip(x, top, x + this.selW, bottom);
    this.selectedRows().forEach((r, i) => {
      const y = top + 2 - Math.round(this.selScroll) + i * SEL_ROW;
      if (y + SEL_ROW <= top || y >= bottom) return;
      if (hit === r || (hit && hit.pack && hit.pack === r.pack)) g.rect(rx, y, rx + rw, y + SEL_ROW, ROW_HOVER);
      if (r.type === 'cat') g.text(clip(r.label, rw - 12), rx + 6, y + 3, DIM);
      else {
        g.rect(rx + 6, y + 3, rx + 8, y + 11, this.accent(r.pack));
        g.text(clip(r.label, rw - 18), rx + 12, y + 3, WHITE);
      }
    });
    g.unclip();
  }

  renderHeaders(g) {
    if (this.sideShown) {
      const room = this.sideW - PAD - ICON_BTN - MARGIN * 2;
      g.text(clip('VTDownloader', room), GAP + PAD, GAP + 6, WHITE);
      g.text(clip('Select Resource Packs', room), GAP + PAD, GAP + 17, DIM);
    }
    if (this.centerShown && !this.searching) {
      const c = this.category, room = this.right - this.left - PAD - 56 - MARGIN * 2;
      const heading = c.parent ? c.parent.name + ' / ' + c.name : c.name, hx = this.left + MARGIN + ICON_BTN * 2 + 2 + MARGIN;
      g.text(clip(heading, room - (hx - this.left - PAD)), hx, GAP + 8, WHITE);
    }
  }

  renderDividers(g, m) {
    this.wantsResize = false;
    const d = this.drag?.which;
    if (d === 'side' || (!d && this.overSide(m))) {
      if (!this.sideHidden) { const l = this.sideDivider(); g.rect(l + 1, GAP, l + GAP - 1, this.panelsBottom, d ? ACCENT : TRACK); }
      this.wantsResize = true;
    } else if (d === 'sel' || (!d && this.overSel(m))) {
      if (!this.selHidden) { const l = this.selDivider(); g.rect(l + 1, GAP, l + GAP - 1, this.panelsBottom, d ? ACCENT : TRACK); }
      this.wantsResize = true;
    }
  }

  renderName(g, m) {
    const b = this.nameBox(), focus = this.focus === 'name';
    if (b.w < 24) return;
    const hover = m.x >= b.x && m.x < b.x + b.w && m.y >= b.y && m.y < b.y + b.h;
    g.box(b.x, b.y, b.x + b.w, b.y + b.h, focus || hover ? CONTROL_HOVER : CONTROL);
    const y = b.y + ((b.h - 7) >> 1), room = b.w - 12;
    if (this.name) {
      const s = clipStart(this.name, room);
      g.text(s, b.x + 6, y, WHITE);
      if (focus && this.caret) g.text('_', b.x + 6 + tw(s), y, WHITE);
    } else {
      if (focus && this.caret) g.text('_', b.x + 6, y, WHITE);
      g.text(clip('Name your resource pack...', room), b.x + 6, y, FAINT);
    }
  }

  renderSearch(g, m) {
    const b = this.searchBox(), focus = this.focus === 'search';
    const hover = m.x >= b.x && m.x < b.x + b.w && m.y >= b.y && m.y < b.y + b.h;
    g.rect(b.x, b.y + b.h - 1, b.x + b.w, b.y + b.h, focus ? ACCENT : hover ? FAINT : TRACK);
    const y = b.y + ((b.h - 7) >> 1);
    if (this.query) {
      const s = clipStart(this.query, b.w);
      g.text(s, b.x, y, WHITE);
      if (focus && this.caret) g.text('_', b.x + tw(s), y, WHITE);
    } else {
      if (focus && this.caret) g.text('_', b.x, y, WHITE);
      g.text(clip('Search all packs...', b.w), b.x, y, FAINT);
    }
  }

  renderPopup(g) {
    const lines = this.popup.lines, w = 186, h = lines.length * 9 + 16, x = (this.w - w) >> 1, y = (this.h - h) >> 1;
    g.rect(x, y, x + w, y + h, 0xFF282828);
    lines.forEach((l, i) => g.text(l, x + 8, y + 8 + i * 9, WHITE));
  }

  renderProgress(g) {
    const w = Math.min(200, this.w - 24), h = 24, x = (this.w - w) >> 1, y = (this.h - h) >> 1;
    g.rect(x, y, x + w, y + h, 0xFF282828);
    g.rect(x + 10, y + 10, x + w - 10, y + 14, CONTROL_OFF);
    g.rect(x + 10, y + 10, x + 10 + Math.floor((w - 20) * Math.min(1, this.progress)), y + 14, ACCENT);
  }

  renderTooltip(g, lines, m) {
    const w = Math.max(...lines.map(tw)) , h = lines.length * 10 - 2 + (lines.length > 1 ? 0 : 0);
    let x = Math.floor(m.x) + 12, y = Math.floor(m.y) - 12;
    if (x + w + 4 > this.w) x = Math.max(4, Math.floor(m.x) - 16 - w);
    if (y + h + 4 > this.h) y = this.h - h - 4;
    if (y < 4) y = 4;
    const bg = 0xF0100010;
    g.rect(x - 3, y - 4, x + w + 3, y - 3, bg);
    g.rect(x - 3, y + h + 3, x + w + 3, y + h + 4, bg);
    g.rect(x - 3, y - 3, x + w + 3, y + h + 3, bg);
    g.rect(x - 4, y - 3, x - 3, y + h + 3, bg);
    g.rect(x + w + 3, y - 3, x + w + 4, y + h + 3, bg);
    g.rect(x - 3, y - 2, x - 2, y + h + 2, 0x505000FF);
    g.rect(x + w + 2, y - 2, x + w + 3, y + h + 2, 0x505000FF);
    g.rect(x - 3, y - 3, x + w + 3, y - 2, 0x505000FF);
    g.rect(x - 3, y + h + 2, x + w + 3, y + h + 3, 0x5028007F);
    lines.forEach((l, i) => g.text(l, x, y + i * 10, WHITE));
  }

  // Where things are, for the scripted tours
  packPoint(id) {
    if (!this.rows) this.buildRows();
    const off = this.listTop + 2 - Math.round(this.scroll), x0 = this.left + PAD;
    for (const r of this.rows) {
      if (r.type === 'pack' && r.pack.id === id) return { x: x0 + 20, y: off + r.top + 20, top: r.top, h: r.h, arrow: { x: x0 + this.rowWidth - 6 - (ARROW >> 1), y: off + r.top + (r.h >> 1) } };
      if (r.type === 'tiles') {
        const i = r.tiles.findIndex(p => p.id === id);
        if (i >= 0) { const t = Math.floor((this.rowWidth - 4 * (r.cols - 1)) / r.cols); return { x: x0 + i * (t + 4) + (t >> 1), y: off + r.top + 20, top: r.top, h: r.h }; }
      }
    }
    return null;
  }
  buttonPoint(id) {
    const b = this.buttons().find(e => e.id === id);
    return b && { x: b.x + (b.w >> 1), y: b.y + (b.h >> 1) };
  }
  categoryPoint(name) {
    if (name === 'Installed') return { x: GAP + 30, y: this.installedY + 8 };
    const i = this.sideRows().findIndex(r => r.cat.name === name);
    return { x: GAP + 30, y: this.catY(i) + 8 };
  }
}

/** The small button that opens a pack's page or a subcategory: quiet until its row is hovered. */
function arrowButton(g, x, y, rowHover, over) {
  if (over) g.box(x, y, x + ARROW, y + ARROW, ACCENT);
  else if (rowHover) g.box(x, y, x + ARROW, y + ARROW, CONTROL_HOVER);
  g.pixels(ICONS.arrow, x + ((ARROW - 6) >> 1), y + ((ARROW - 5) >> 1), over ? BLACK : rowHover ? WHITE : FAINT);
}
function rootOf(c) { while (c.parent) c = c.parent; return c; }
/** Removing a link's tags leaves the spaces around it, as in "OptiFine , or", and lines can start indented. */
function tidy(t) { return (t || '').replace(/ +([,.!?;:)])/g, '$1').replace(/\n[ \t]+/g, '\n').trim(); }
function outline(g, x0, y0, x1, y1) {
  g.rect(x0, y0, x1, y0 + 1, WHITE); g.rect(x0, y1 - 1, x1, y1, WHITE);
  g.rect(x0, y0, x0 + 1, y1, WHITE); g.rect(x1 - 1, y0, x1, y1, WHITE);
}
function clipStart(s, w) { while (s && tw(s) + 6 > w) s = s.slice(1); return s; }
const isMac = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent);

/** GuiGraphics in GUI pixels. */
class Graphics {
  constructor(ctx) { this.ctx = ctx; }
  rect(x0, y0, x1, y1, c) { this.ctx.fillStyle = css(c >>> 0); this.ctx.fillRect(x0, y0, x1 - x0, y1 - y0); }
  box(x0, y0, x1, y1, c) { this.rect(x0 + 1, y0, x1 - 1, y1, c); this.rect(x0, y0 + 1, x0 + 1, y1 - 1, c); this.rect(x1 - 1, y0 + 1, x1, y1 - 1, c); }
  text(s, x, y, c) { if (!s) return; this.ctx.font = `${fontPx}px Monocraft`; this.ctx.fillStyle = css(c >>> 0); this.ctx.fillText(s, x, y + 7); }
  icon(id, x, y, size) {
    const e = icon(id);
    if (e.ok) this.ctx.drawImage(e.img, x, y, size, size);
    else this.rect(x, y, x + size, y + size, ROW_HOVER);
  }
  pixels(rows, x, y, c) {
    this.ctx.fillStyle = css(c >>> 0);
    rows.forEach((row, j) => { for (let i = 0; i < row.length; i++) if (row[i] === 'X') this.ctx.fillRect(x + i, y + j, 1, 1); });
  }
  clip(x0, y0, x1, y1) { this.ctx.save(); this.ctx.beginPath(); this.ctx.rect(x0, y0, x1 - x0, y1 - y0); this.ctx.clip(); }
  unclip() { this.ctx.restore(); }
}

class Stop extends Error {}

const ease = t => t < .5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;

/** Moves the fake pointer, clicks and types for a script; stops as soon as a person touches the screen. */
class Director {
  constructor(view) { this.view = view; this.run = 0; this.paused = false; }
  get s() { return this.view.screen; }
  caption(text) { this.view.setCaption(text); }
  frame() { return new Promise(r => requestAnimationFrame(r)); }
  async step(run) {
    do {
      if (run !== this.run) throw new Stop();
      await this.frame();
    } while (this.paused || !this.view.visible);
    if (run !== this.run) throw new Stop();
  }
  async wait(run, ms) {
    let left = ms, last = performance.now();
    while (left > 0) {
      await this.step(run);
      const now = performance.now();
      left -= now - last; last = now;
    }
  }
  async go(run, p, ms = 650) {
    const from = { ...this.view.pointer };
    let t = 0, last = performance.now();
    while (t < 1) {
      await this.step(run);
      const now = performance.now();
      t = Math.min(1, t + (now - last) / ms); last = now;
      const k = ease(t);
      this.view.moveFake(from.x + (p.x - from.x) * k, from.y + (p.y - from.y) * k);
    }
  }
  async click(run, p, ms) {
    if (p) await this.go(run, p, ms);
    await this.wait(run, 140);
    this.view.pressing = true;
    this.s.pointerDown({ ...this.view.pointer });
    await this.wait(run, 110);
    this.view.pressing = false;
    this.s.pointerUp();
    this.s.dirty = true;
    await this.wait(run, 160);
  }
  async type(run, text, field) {
    for (const ch of text) {
      if (field === 'search') this.s.setQuery(this.s.query + ch); else { this.s.name += ch; this.s.dirty = true; }
      await this.wait(run, 70 + Math.random() * 60);
    }
  }
  async drag(run, from, to, ms = 800) {
    await this.go(run, from);
    await this.wait(run, 180);
    this.view.pressing = true;
    this.s.pointerDown({ ...from });
    await this.go(run, to, ms);
    await this.wait(run, 120);
    this.s.pointerUp();
    this.view.pressing = false;
    this.s.dirty = true;
  }
  /** Scrolls the list until the row is in view. */
  async reveal(run, id) {
    const s = this.s, p = s.packPoint(id);
    if (!p) return;
    const target = Math.max(0, Math.min(s.maxScroll, p.top - 40)), from = s.scroll;
    let t = 0, last = performance.now();
    while (t < 1 && from !== target) {
      await this.step(run);
      const now = performance.now();
      t = Math.min(1, t + (now - last) / 500); last = now;
      s.scroll = from + (target - from) * ease(t); s.dirty = true;
    }
  }
  start(script) {
    const run = ++this.run;
    this.view.scripted = true;
    this.s.quietTips = true;
    this.paused = false;
    script(this, run).catch(e => { if (!(e instanceof Stop)) throw e; });
  }
  stop() { this.run++; this.view.scripted = false; this.view.pressing = false; this.s.quietTips = false; this.s.dirty = true; }
}

const SCRIPTS = {
  async tour(d, run) {
    for (;;) {
      const s = d.s;
      d.view.resetScreen();
      d.view.moveFake(s.w * .55, s.h * .9);
      d.caption('Pick a category, then click the packs you want.');
      await d.wait(run, 600);
      await d.click(run, s.categoryPoint('Terrain'));
      await d.wait(run, 300);
      for (const id of s.category.packs.slice(1, 3).map(p => p.id)) { await d.click(run, s.packPoint(id), 500); await d.wait(run, 200); }
      await d.wait(run, 500);

      d.caption(touch() ? 'Search looks through every category at once.' : `${isMac ? 'Cmd' : 'Ctrl'}+F searches every category at once.`);
      await d.wait(run, 400);
      s.openSearch(); await d.wait(run, 350);
      await d.type(run, 'honey', 'search');
      await d.wait(run, 500);
      await d.click(run, s.packPoint('SolidHoney'));
      await d.wait(run, 500);
      s.closeSearch();
      await d.wait(run, 200);

      d.caption('Back and forward work like a browser, and return to where you scrolled.');
      await d.click(run, s.categoryPoint('GUI'));
      await d.wait(run, 500);
      await d.reveal(run, s.category.packs.at(-1).id);
      await d.wait(run, 400);
      await d.click(run, s.buttonPoint('back'));
      await d.wait(run, 650);
      await d.click(run, s.buttonPoint('forward'));
      await d.wait(run, 650);

      if (!touch()) {
        d.caption('Arrow keys move through the packs. Space picks one.');
        await d.wait(run, 400);
        for (let i = 0; i < 3; i++) { s.moveCursor(0, 1); await d.wait(run, 350); }
        s.activateCursor(false);
        await d.wait(run, 700);
        s.cursorShown = false; s.dirty = true;
      }

      d.caption('The arrow opens a pack: its preview, and its whole description.');
      await d.click(run, s.categoryPoint('Aesthetic'));
      await d.wait(run, 300);
      await d.reveal(run, 'GlowierGlow');
      await d.wait(run, 300);
      await d.click(run, s.packPoint('GlowierGlow').arrow);
      await d.wait(run, 3200);
      await d.click(run, s.buttonPoint('back'));
      await d.wait(run, 400);

      d.caption('Installed shows the packs already in your resource pack folder.');
      await d.click(run, s.categoryPoint('Installed'));
      await d.wait(run, 1600);
      await d.click(run, s.categoryPoint('GUI'));
      await d.wait(run, 300);

      d.caption('List or grid. The screen remembers which.');
      await d.click(run, s.buttonPoint('grid'));
      await d.wait(run, 700);
      for (const i of [3, 8]) { const id = s.category.packs[i]?.id; if (id) await d.click(run, s.packPoint(id), 450); }
      await d.wait(run, 500);

      if (!s.narrow) {
        d.caption('Drag the edge of a panel to resize it.');
        await d.wait(run, 200);
        await d.drag(run, { x: s.sideDivider() + 2, y: s.h * .45 }, { x: s.sideDivider() + 40, y: s.h * .45 });
        await d.wait(run, 400);
        d.caption('Drag it all the way and it hides. The arrow brings it back.');
        await d.drag(run, { x: s.sideDivider() + 2, y: s.h * .45 }, { x: 5, y: s.h * .45 }, 900);
        await d.wait(run, 700);
        if (s.selFits) {
          await d.drag(run, { x: s.selDivider() + 2, y: s.h * .4 }, { x: s.w - 5, y: s.h * .4 }, 800);
          await d.wait(run, 900);
          await d.click(run, s.buttonPoint('showSel'));
          await d.wait(run, 300);
        }
        await d.click(run, s.buttonPoint('showSide'));
        await d.wait(run, 400);
      }

      d.caption('Name it and download. The pack lands in your resource packs folder.');
      const n = s.nameBox();
      await d.click(run, { x: n.x + 30, y: n.y + 9 });
      await d.type(run, 'my tweaks', 'name');
      await d.wait(run, 300);
      await d.click(run, s.buttonPoint('download'));
      await d.wait(run, 3700);
      d.caption(touch() ? 'Now try it. Tap anything.' : 'Now try it. Click anything, or use the arrow keys.');
      await d.wait(run, 4000);
    }
  },
  async search(d, run) {
    const words = [['honey', 'SolidHoney'], ['stone', null], ['crosshair', null]];
    for (;;) {
      for (const [word, pick] of words) {
        const s = d.s;
        d.view.resetScreen();
        d.view.moveFake(s.w * .7, s.h * .75);
        await d.wait(run, 500);
        s.openSearch(); await d.wait(run, 300);
        await d.type(run, word, 'search');
        await d.wait(run, 700);
        if (pick) { await d.click(run, s.packPoint(pick)); }
        else { await d.go(run, { x: s.w * .55, y: s.h * .5 }); }
        await d.wait(run, 1300);
      }
    }
  },
  async resize(d, run) {
    for (;;) {
      const s = d.s;
      d.view.resetScreen();
      d.view.moveFake(s.w * .6, s.h * .8);
      await d.wait(run, 500);
      await d.drag(run, { x: s.sideDivider() + 2, y: s.h * .4 }, { x: s.sideDivider() + 44, y: s.h * .4 });
      await d.wait(run, 450);
      await d.drag(run, { x: s.sideDivider() + 2, y: s.h * .4 }, { x: 70, y: s.h * .4 }, 600);
      await d.wait(run, 450);
      await d.drag(run, { x: s.sideDivider() + 2, y: s.h * .4 }, { x: 5, y: s.h * .4 }, 600);
      await d.wait(run, 900);
      await d.click(run, s.buttonPoint('showSide'));
      await d.wait(run, 1200);
    }
  },
  async conflict(d, run) {
    for (;;) {
      const s = d.s;
      d.view.resetScreen();
      d.view.moveFake(s.w * .7, s.h * .8);
      const pair = ['AlternateBlockDestruction', 'MineProgressBar'];
      await d.wait(run, 400);
      d.s.setCategory(d.s.data.packs.get(pair[0]).cat);
      await d.wait(run, 300);
      await d.click(run, s.packPoint(pair[0]));
      await d.wait(run, 700);
      s.openSearch(); await d.wait(run, 250);
      await d.type(run, 'progress', 'search');
      await d.wait(run, 300);
      await d.click(run, s.packPoint(pair[1]));
      await d.wait(run, 400);
      s.closeSearch();
      await d.wait(run, 2200);
    }
  },
};

const OPTS = {
  tour: { target: 500, interactive: true, opts: { selW: 126 } },
  search: { target: 500, label: 'Animation of searching for packs', opts: { hideSide: true, hideSelected: true } },
  resize: { target: 500, label: 'Animation of resizing and hiding a side panel', minW: NARROW, needs: 'of dragging a side panel', opts: { hideSelected: true, sideW: 90 } },
  conflict: { target: 500, label: 'Animation of two conflicting packs changing colour', minW: SEL_FIT, needs: 'of the Selected packs panel', opts: { hideSide: true } },
};

/** Turns a {@code data-screen} element into a live screen. */
class View {
  constructor(root, data) {
    this.root = root;
    const name = root.dataset.screen;
    const conf = OPTS[name] || OPTS.tour;
    this.name = name; this.conf = conf;
    root.innerHTML = '';
    root.classList.add('is-live');
    this.canvas = document.createElement('canvas');
    this.interactive = !!conf.interactive;
    root.classList.toggle('is-interactive', this.interactive);
    if (this.interactive) {
      this.canvas.tabIndex = 0;
      this.canvas.setAttribute('role', 'application');
      this.canvas.setAttribute('aria-label', 'A working copy of the VTDownloader screen');
    } else {
      this.canvas.setAttribute('role', 'img');
      this.canvas.setAttribute('aria-label', conf.label);
    }
    // What this display can't show: above the screen when it's cut down, or in its place when it can't work at all
    this.notice = document.createElement('div');
    this.notice.className = 'screen__notice';
    this.notice.hidden = true;
    root.append(this.notice);
    this.input = document.createElement('input');
    Object.assign(this.input, { type: 'text', tabIndex: -1, autocomplete: 'off', spellcheck: false, className: 'screen__keys' });
    this.input.setAttribute('aria-hidden', 'true');
    root.append(this.canvas, this.input);
    if (root.hasAttribute('data-bar')) {
      const bar = document.createElement('div');
      bar.className = 'screen__bar';
      this.pauseBtn = el('button', 'Pause');
      this.note = el('span', ''); this.note.className = 'screen__note'; this.note.setAttribute('aria-live', 'off');
      bar.append(this.pauseBtn, this.note);
      if (this.interactive) {
        this.restart = el('button', 'Restart');
        bar.append(this.restart);
        this.restart.onclick = () => this.play();
      }
      root.append(bar);
      this.pauseBtn.onclick = () => {
        if (!this.director.view.scripted) return this.play();
        this.director.paused = !this.director.paused;
        this.pauseBtn.textContent = this.director.paused ? 'Play' : 'Pause';
      };
    }
    this.screen = new Screen(data, conf.opts);
    live.add(this.screen);
    this.ctx = this.canvas.getContext('2d', { alpha: false });
    this.g = new Graphics(this.ctx);
    this.pointer = { x: -20, y: -20 };
    this.visible = false;
    this.director = new Director(this);
    if (this.interactive) this.bind();
    new ResizeObserver(() => this.fit()).observe(root);
    new IntersectionObserver(es => {
      this.visible = es.at(-1).isIntersecting;
      if (this.visible) { this.kick(); if (!this.begun) { this.begun = true; this.begin(); } }
    }, { threshold: .2 }).observe(this.canvas);
    this.fit();
  }

  begin() {
    if (matchMedia('(prefers-reduced-motion: reduce)').matches) {
      if (this.interactive) this.setCaption(touch() ? 'Tap anything.' : 'Click anything. Press ' + (isMac ? 'Cmd' : 'Ctrl') + '+F to search.');
      else this.setCaptionState('Play');
      return;
    }
    this.play();
  }

  play() {
    this.setCaptionState('Pause');
    if (SCRIPTS[this.name]) this.director.start(SCRIPTS[this.name]);
  }

  setCaptionState(label) { if (this.pauseBtn) this.pauseBtn.textContent = label; }
  setCaption(text) { if (this.note) this.note.textContent = text; }

  /** Hand over to the person: the script stops and the real pointer takes over. */
  takeOver() {
    if (!this.director.view.scripted) return;
    this.director.stop();
    this.setCaptionState('Replay');
    this.setCaption(touch() ? 'It’s yours. Tap categories and packs, and scroll the list. Nothing downloads.' : 'It’s yours. Drag the panel edges, pick packs, press ' + (isMac ? 'Cmd' : 'Ctrl') + '+F. Nothing downloads.');
    this.root.classList.add('is-yours');
  }

  resetScreen() {
    this.screen.reset({ ...this.conf.opts, hideSide: this.conf.opts?.hideSide ?? false });
  }

  moveFake(x, y) { this.pointer = { x, y }; this.screen.mouse = { x, y }; if (this.screen.drag) this.screen.pointerMove({ x, y }); this.screen.dirty = true; }

  fit() {
    const cssW = this.root.clientWidth;
    if (!cssW) return;
    const dpr = window.devicePixelRatio || 1, dev = Math.round(cssW * dpr);
    const scale = Math.max(1, Math.round(dev / this.conf.target), Math.ceil(1.5 * dpr));
    this.scale = scale;
    const w = Math.floor(dev / scale);
    const hero = this.name === 'tour';
    const cap = hero ? Math.floor(innerHeight * .78 * dpr / scale) : Infinity;
    const h = Math.max(hero ? 170 : 120, Math.min(Math.round(w * (.56)), cap));
    this.canvas.width = w * scale;
    this.canvas.height = h * scale;
    this.canvas.style.width = (w * scale / dpr) + 'px';
    this.canvas.style.height = (h * scale / dpr) + 'px';
    if (w < NARROW && !this.seenNarrow) { this.seenNarrow = true; this.screen.sideHidden = true; this.screen.selHidden = true; }
    this.updateNotice(w, scale / dpr);
    this.screen.resize(w, h);
    this.kick();
    this.draw();
  }

  /** Says what this display leaves out, so a smaller screen doesn't look like the whole mod. */
  updateNotice(w, cssScale) {
    const small = w < NARROW || touch();
    const missing = [];
    if (this.interactive) {
      if (w < NARROW) missing.push('The side panels open one at a time from the arrows, and can’t be resized or hidden.');
      else if (w < SEL_FIT) missing.push('The Selected packs panel doesn’t fit at this width. The game leaves it out too.');
      if (touch()) missing.push('Touch can’t drag panel edges, use keyboard shortcuts, or show tooltips.');
    }
    const tooSmall = !!this.conf.minW && w < this.conf.minW;
    this.root.classList.toggle('is-too-small', tooSmall);
    this.notice.replaceChildren();
    this.notice.hidden = !missing.length && !tooSmall;
    if (tooSmall) {
      this.notice.append(el('p', `Not shown on this display. This animation ${this.conf.needs} needs a window at least ${Math.ceil(this.conf.minW * cssScale)} pixels wide.`));
    } else if (missing.length) {
      this.notice.append(el('p', small
        ? 'This mod runs in Minecraft: Java Edition on a computer. Phones and tablets aren’t supported. This copy of the screen is missing some things here:'
        : 'This window is too narrow for the whole screen:'));
      const list = document.createElement('ul');
      for (const line of missing) list.append(el('li', line));
      this.notice.append(list);
    }
  }

  guiPoint(e) {
    const r = this.canvas.getBoundingClientRect();
    return { x: (e.clientX - r.left) * this.screen.w / r.width, y: (e.clientY - r.top) * this.screen.h / r.height };
  }

  bind() {
    const c = this.canvas, s = () => this.screen;
    c.addEventListener('pointerdown', e => {
      this.takeOver();
      const m = this.guiPoint(e);
      c.setPointerCapture(e.pointerId);
      if (e.pointerType === 'touch') {
        this.touch = { x: e.clientY, last: e.clientY, moved: false, m };
        s().pointerMove(m);
      } else if (e.button === 3 || e.button === 4) {
        e.button === 3 ? s().goBack() : s().goForward();
      } else if (e.button === 0) {
        s().pointerMove(m);
        s().pointerDown(m, e.shiftKey);
      }
      this.syncInput(true);
      e.preventDefault();
      if (!s().focus) c.focus({ preventScroll: true });
    });
    c.addEventListener('pointermove', e => {
      const m = this.guiPoint(e);
      if (this.touch) {
        const dy = e.clientY - this.touch.last;
        if (Math.abs(e.clientY - this.touch.x) > 4) this.touch.moved = true;
        this.touch.last = e.clientY;
        if (this.touch.moved) s().wheel(m, -dy * 2);
        return;
      }
      if (this.director.view.scripted) return;
      this.pointer = m;
      s().pointerMove(m);
    });
    c.addEventListener('pointerup', e => {
      if (this.touch) {
        if (!this.touch.moved) { s().pointerDown(this.touch.m); this.syncInput(true); }
        this.touch = null;
        s().mouse = { x: -1, y: -1 };
        s().dirty = true;
      }
      s().pointerUp();
    });
    c.addEventListener('pointerleave', () => { if (!this.director.view.scripted && !s().drag) { s().mouse = { x: -1, y: -1 }; s().dirty = true; } });
    c.addEventListener('wheel', e => {
      if (this.director.view.scripted && !this.over) { /* falls through to the page */ }
      const m = this.guiPoint(e);
      // Only take the wheel from the page when the list can use it
      if (!this.director.view.scripted && document.activeElement && (document.activeElement === c || document.activeElement === this.input)) {
        e.preventDefault();
        s().wheel(m, e.deltaY, e.deltaX, e.shiftKey);
      }
    }, { passive: false });
    c.addEventListener('pointerenter', () => { this.over = true; });
    c.addEventListener('pointerleave', () => { this.over = false; });
    this.input.addEventListener('input', () => {
      const v = this.input.value;
      if (s().focus === 'search') s().setQuery(v);
      else if (s().focus === 'name') { s().name = v; s().dirty = true; }
    });
    const keys = e => {
      const key = e.key;
      if ((e.metaKey || e.ctrlKey) && key.toLowerCase() === 'f') {
        this.takeOver();
        e.preventDefault();
        s().openSearch();
        this.syncInput(true);
        return;
      }
      if ((e.metaKey || e.ctrlKey) && (key === '[' || key === ']')) {
        this.takeOver();
        e.preventDefault();
        key === '[' ? s().goBack() : s().goForward();
        this.syncInput(true);
        return;
      }
      if (e.ctrlKey && key === 'Tab') {
        this.takeOver(); e.preventDefault();
        s().adjacentCategory(e.shiftKey ? -1 : 1);
        this.syncInput(true);
        return;
      }
      if ((e.metaKey || e.ctrlKey) && /^[1-9]$/.test(key)) {
        const c = s().topCats[Number(key) - 1];
        this.takeOver(); e.preventDefault();
        if (c) { s().setCategory(c); this.syncInput(true); }
        return;
      }
      // Arrow keys move a cursor over the packs; Space or Enter picks the one it's on. The name field keeps its keys,
      // and the search field keeps Left, Right and Space for typing
      if (!e.metaKey && !e.ctrlKey && !e.altKey && s().focus !== 'name') {
        const inSearch = s().focus === 'search';
        const move = { ArrowUp: [0, -1], ArrowDown: [0, 1], ArrowLeft: [-1, 0], ArrowRight: [1, 0] }[key];
        if (move && !(inSearch && move[0])) {
          this.takeOver(); e.preventDefault();
          s().moveCursor(move[0], move[1]);
          return;
        }
        if ((key === 'Enter' || (key === ' ' && !inSearch)) && s().cursorShown) {
          this.takeOver(); e.preventDefault();
          s().activateCursor(e.shiftKey);
          return;
        }
      }
      if ((e.metaKey || e.ctrlKey) && key.toLowerCase() === 'a' && !s().focus) {
        this.takeOver();
        e.preventDefault();
        e.shiftKey ? s().clearSelection() : s().selectAllShown();
        return;
      }
      if (key === 'Escape') {
        if (s().details && !s().focus) { s().closeDetails(); e.preventDefault(); e.stopPropagation(); }
        else if (s().searching) { s().closeSearch(); this.syncInput(true); e.preventDefault(); e.stopPropagation(); }
        else if (s().focus) { s().focus = null; s().dirty = true; this.syncInput(true); }
      }
    };
    c.addEventListener('keydown', keys);
    this.input.addEventListener('keydown', keys);
    // Cmd/Ctrl+F belongs to this screen while the pointer is over it; everywhere else the browser's find still works
    window.addEventListener('keydown', e => {
      if (this.over && (e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'f' && document.activeElement !== c && document.activeElement !== this.input) keys(e);
    });
  }

  syncInput(focus) {
    const s = this.screen;
    if (s.focus) {
      this.input.value = s.focus === 'search' ? s.query : s.name;
      if (focus) this.input.focus({ preventScroll: true });
    } else if (document.activeElement === this.input) this.canvas.focus({ preventScroll: true });
  }

  kick() {
    if (this.running) return;
    this.running = true;
    let last = performance.now();
    const loop = now => {
      if (!this.visible && !this.director.view.scripted) { this.running = false; return; }
      const dt = now - last; last = now;
      this.screen.tick(now, dt);
      if (this.screen.dirty) this.draw();
      requestAnimationFrame(loop);
    };
    requestAnimationFrame(loop);
  }

  draw() {
    const s = this.screen;
    s.dirty = false;
    this.ctx.setTransform(this.scale, 0, 0, this.scale, 0, 0);
    this.ctx.imageSmoothingEnabled = false;
    s.render(this.g);
    this.canvas.style.cursor = s.drag || s.wantsResize ? 'ew-resize' : 'default';
    if (this.director.view.scripted) {
      const x = Math.round(this.pointer.x), y = Math.round(this.pointer.y);
      CURSOR.forEach((row, j) => {
        for (let i = 0; i < row.length; i++) {
          if (row[i] === '.') continue;
          this.ctx.fillStyle = row[i] === 'X' ? '#000' : (this.pressing ? '#E99743' : '#fff');
          this.ctx.fillRect(x + i, y + j, 1, 1);
        }
      });
    }
  }
}

function el(tag, text) { const e = document.createElement(tag); e.textContent = text; if (tag === 'button') e.type = 'button'; return e; }
function touch() { return matchMedia('(pointer: coarse)').matches; }

export async function mountAll(base) {
  const roots = [...document.querySelectorAll('[data-screen]')];
  if (!roots.length) return;
  try {
    const [data] = await Promise.all([loadData(base), loadFont(base + 'fonts/Monocraft.otf')]);
    widths.clear();
    roots.forEach(r => new View(r, data));
  } catch (e) {
    console.error(e);
    roots.forEach(r => r.classList.add('is-failed'));
  }
}

mountAll(new URL('./', import.meta.url).href);
