package mix.vtdrefresh.gui.widget;

import me.bymartrixx.vtd.VTDMod;
import me.bymartrixx.vtd.access.TextureManagerAccess;
import me.bymartrixx.vtd.data.Category;
import me.bymartrixx.vtd.data.Pack;
import mix.vtdrefresh.gui.PackLayout;
import mix.vtdrefresh.gui.ScreenSettings;
import mix.vtdrefresh.gui.Theme;
import mix.vtdrefresh.gui.RefreshScreen;
import me.bymartrixx.vtd.util.Constants;
import me.bymartrixx.vtd.util.RenderUtil;
import me.bymartrixx.vtd.util.Util;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.NativeImage;
import java.net.URI;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class PackSelectionListWidget extends AbstractSelectionList<PackSelectionListWidget.AbstractEntry> {
    // DEBUG
    private static final boolean SHOW_DEBUG_INFO = false;
    private static final boolean DEBUG_ERROR_TEXT = false;
    private static final boolean DISABLE_ICONS = false;

    private static final Component ERROR_URL = Util.urlText(VTDMod.BASE_URL);
    private static final Component ERROR_HEADER = Component.translatable("vtd.packError.title")
            .withStyle(ChatFormatting.BOLD, ChatFormatting.ITALIC);
    private static final Component ERROR_BODY = Component.translatable("vtd.packError.body", ERROR_URL);
    private static final Component NO_RESULTS_TEXT = Component.translatable("vtd.search.noResults");
    private static final Component ALL_TEXT = Component.translatable("vtd.installed.all");
    private static final Component NO_INSTALLED_TEXT = Component.translatable("vtd.installed.empty");
    private static final Component ERROR_TEXT = Component.empty().append(ERROR_HEADER).append("\n").append(ERROR_BODY);

    public static final int ITEM_HEIGHT = 44;
    private static final int WARNING_MARGIN = 10;
    private static final int WARNING_BG_MARGIN = 2;
    private static final int ROW_LEFT_RIGHT_MARGIN = 8;
    private static final int TEXT_MARGIN = 2;
    private static final int ICON_SIZE = 32;
    private static final int ICON_LEFT = 6;
    private static final int ICON_RIGHT = 8;
    private static final int ACCENT_WIDTH = 2;
    /** Space above and below a row's text. */
    private static final int ROW_PADDING = 6;
    private static final int WARNING_PADDING = 6;
    private static final int NOTE_PADDING = 4;
    private static final int TILE_MIN_WIDTH = 72;
    private static final int TILE_GAP = 4;
    private static final int TILE_ROW_HEIGHT = 60;
    private static final int TILE_TOP = 4;
    private static final int TILE_NAME_TOP = 40;
    private static final int TILE_NAME_MARGIN = 3;
    /** Lines of description a list row shows; the rest is on the pack's page. */
    private static final int DESCRIPTION_LINES = 2;
    private static final int ARROW_MARGIN = 6;
    private static final int TILE_ARROW_INSET = 3;
    private static final int SUBCATEGORY_ROW_HEIGHT = 26;
    private static final int SUBCATEGORY_ICON_SIZE = 16;
    private static final float SELECTED_TINT = 0.2F;
    private static final float SELECTED_HOVER_TINT = 0.3F;

    private final Map<Category, List<AbstractEntry>> entryCache = new HashMap<>();
    private final RefreshScreen screen;
    private Category category;
    private boolean editable = true;
    private PackLayout layout = ScreenSettings.getPackLayout();
    private List<Category> categories = List.of();
    private List<String> searchTerms = null;
    private int tileColumns;
    /** The row width the list was last laid out for. In the list layout, row heights follow the text's wrapping. */
    private int layoutWidth;

    /** Stands in for the packs found in the resource pack folder, as the sidebar's Installed row. */
    @Nullable
    private Category installedCategory;
    /** Each Vanilla Tweaks zip in the folder, by file name, with the pack ids it lists. */
    private Map<String, List<String>> installedSources = Map.of();
    /** The zip whose packs are shown, or null for all of them. */
    @Nullable
    private String installedFilter;
    @Nullable
    private ChipRowEntry installedChips;

    private final MultiLineLabel errorText;

    /** The pack whose page covers the list, opened from its arrow. */
    @Nullable
    private PackDetails details;

    private final PackSelectionHelper selectionHelper;
    @Nullable
    private Pack selectionAnchor;
    /** The pack the arrow keys are on. Shown only once they've been used, until the mouse is. */
    @Nullable
    private Pack cursor;
    private boolean cursorShown;

    public PackSelectionListWidget(Minecraft client, RefreshScreen screen, int x, int width, int height, int y,
                                   Category category, PackSelectionHelper selectionHelper) {
        super(client, width, height, y, ITEM_HEIGHT);
        this.setX(x);
        this.screen = screen;
        this.category = category;
        this.selectionHelper = selectionHelper;

        this.errorText = Util.createMultilineText(client.font, ERROR_TEXT, 8, (int) (width / 1.5));

        this.showEntries(this.getPackEntries(category));
    }

    public void setCategory(Category category) {
        this.category = category;
        this.closeDetails();

        this.searchTerms = null;
        this.setFocused(null);
        this.showEntries(this.getPackEntries(category));
        this.setScrollAmount(0.0);
    }

    /** The packs in the Vanilla Tweaks zips in the resource pack folder, by zip. */
    public void setInstalled(Category installedCategory, Map<String, List<String>> bySource) {
        this.installedCategory = installedCategory;
        if (!bySource.equals(this.installedSources)) {
            this.installedSources = bySource;
            this.installedChips = null;
            if (this.installedFilter != null && !bySource.containsKey(this.installedFilter)) {
                this.installedFilter = null;
            }
        }
        if (this.category == installedCategory && this.searchTerms == null) {
            double scroll = this.scrollAmount();
            this.refreshEntries();
            this.setScrollAmount(scroll);
        }
    }

    public void setLayout(PackLayout layout) {
        if (this.layout != layout) {
            this.layout = layout;
            this.refreshEntries();
            this.setScrollAmount(0.0);
        }
    }

    /** Shows the packs of every category matching {@code query}, or the current category's for a blank one. */
    public void setSearch(String query) {
        this.closeDetails();
        this.searchTerms = query.isBlank() ? null : List.of(query.strip().toLowerCase(Locale.ROOT).split("\\s+"));
        this.refreshEntries();
        this.setScrollAmount(0.0);
    }

    /** To be called after the list's width changes, as that can change how many tiles fit in a row. */
    public void onWidthChanged() {
        boolean changed = this.layout == PackLayout.GRID
                ? this.getTileColumns() != this.tileColumns
                : this.getRowWidth() != this.layoutWidth;
        if (changed) {
            double scroll = this.scrollAmount();
            this.refreshEntries();
            this.setScrollAmount(scroll);
        }
    }

    private void refreshEntries() {
        this.setFocused(null);
        this.showEntries(this.searchTerms != null ? this.getSearchResults() : this.getPackEntries(this.category));
    }

    private List<AbstractEntry> getSearchResults() {
        List<AbstractEntry> results = new ArrayList<>();
        for (Category category : this.categories) {
            this.collectSearchResults(category, results);
        }

        return results;
    }

    private void collectSearchResults(Category category, List<AbstractEntry> results) {
        for (AbstractEntry entry : this.getPackEntries(category)) {
            if (entry instanceof PackEntry pack && pack.matches(this.searchTerms)) {
                results.add(pack);
            }
        }

        if (category.getSubCategories() != null) {
            for (Category.SubCategory subCategory : category.getSubCategories()) {
                this.collectSearchResults(subCategory, results);
            }
        }
    }

    /** Fills the list with {@code entries}, grouping the packs in them into rows of tiles for the grid layout. */
    private void showEntries(List<AbstractEntry> entries) {
        this.clearEntries();
        if (this.cursor != null && entries.stream().noneMatch(e -> e instanceof PackEntry p && p.pack == this.cursor)) {
            this.cursor = null;
        }
        this.layoutWidth = this.getRowWidth();

        if (this.layout == PackLayout.LIST) {
            entries.forEach(this::addPlainEntry);
            return;
        }

        int columns = this.getTileColumns();
        this.tileColumns = columns;
        List<PackEntry> tiles = new ArrayList<>();
        List<SubCategoryEntry> boxes = new ArrayList<>();
        for (AbstractEntry entry : entries) {
            if (entry instanceof PackEntry pack) {
                this.addBoxRow(boxes, columns);
                tiles.add(pack);
                if (tiles.size() == columns) {
                    this.addTileRow(tiles, columns);
                }
            } else if (entry instanceof SubCategoryEntry box) {
                this.addTileRow(tiles, columns);
                boxes.add(box);
                if (boxes.size() == columns) {
                    this.addBoxRow(boxes, columns);
                }
            } else {
                this.addTileRow(tiles, columns);
                this.addBoxRow(boxes, columns);
                this.addPlainEntry(entry);
            }
        }
        this.addTileRow(tiles, columns);
        this.addBoxRow(boxes, columns);
    }

    private void addBoxRow(List<SubCategoryEntry> boxes, int columns) {
        if (!boxes.isEmpty()) {
            this.addEntry(new SubCategoryRowEntry(this, List.copyOf(boxes), columns), TILE_ROW_HEIGHT);
            boxes.clear();
        }
    }

    private void addPlainEntry(AbstractEntry entry) {
        this.addEntry(entry, entry.getRowHeight(this.layoutWidth));
    }

    private void addTileRow(List<PackEntry> tiles, int columns) {
        if (!tiles.isEmpty()) {
            this.addEntry(new TileRowEntry(this, List.copyOf(tiles), columns), TILE_ROW_HEIGHT);
            tiles.clear();
        }
    }

    private int getTileColumns() {
        return Math.max(1, (this.getRowWidth() + TILE_GAP) / (TILE_MIN_WIDTH + TILE_GAP));
    }

    public void updateCategories(List<Category> categories) {
        this.categories = new ArrayList<>(categories);
        this.selectionHelper.buildIncompatibilityGroups(categories);
    }

    private List<AbstractEntry> getPackEntries(Category category) {
        if (category == null || DEBUG_ERROR_TEXT) {
            return Collections.emptyList();
        }

        if (category == this.installedCategory) {
            return this.getInstalledEntries();
        }

        if (this.entryCache.containsKey(category)) {
            return this.entryCache.get(category);
        }

        List<AbstractEntry> entries = new ArrayList<>();

        if (category.hasWarning()) {
            //noinspection ConstantConditions
            entries.add(new WarningEntry(this, category.getWarning()));
        }

        for (Pack pack : category.getPacks()) {
            // Experimental packs aren't shown in the web page, at least for now
            if (pack.isExperimental()) {
                continue;
            }

            PackEntry entry = new PackEntry(this, pack, category);
            entries.add(entry);
            if (this.selectionHelper.isSelected(pack)) {
                entry.selectionData.toggleSelection();
            }
        }

        // Subcategories come last, as on vanillatweaks.net
        if (category.getSubCategories() != null) {
            for (Category.SubCategory subCategory : category.getSubCategories()) {
                entries.add(new SubCategoryEntry(this, subCategory));
            }
        }

        this.entryCache.put(category, entries);

        return entries;
    }

    /**
     * A row of chips, one per zip and All, then the chosen zip's packs in the order the categories list them,
     * each once. Not cached, as the folder can change.
     */
    private List<AbstractEntry> getInstalledEntries() {
        List<AbstractEntry> entries = new ArrayList<>();
        if (this.installedSources.isEmpty()) {
            return entries;
        }

        if (this.installedChips == null) {
            this.installedChips = new ChipRowEntry(this, this.createChips());
        }
        entries.add(this.installedChips);

        Set<String> ids = new HashSet<>(this.installedFilter != null
                ? this.installedSources.get(this.installedFilter)
                : this.installedSources.values().stream().flatMap(List::stream).toList());
        entries.addAll(this.findInstalled(ids));
        return entries;
    }

    private List<Chip> createChips() {
        List<Chip> chips = new ArrayList<>();
        if (this.installedSources.size() > 1) {
            Set<String> all = new HashSet<>();
            this.installedSources.values().forEach(all::addAll);
            chips.add(new Chip(null, ALL_TEXT.getString(), this.findInstalled(all).size()));
        }
        this.installedSources.forEach((file, ids) ->
                chips.add(new Chip(file, chipLabel(file), this.findInstalled(new HashSet<>(ids)).size())));
        return chips;
    }

    /** "VanillaTweaks_r147040_MC26.2.x.zip" reads as "r147040_MC26.2.x"; a name of the player's own stays whole. */
    private static String chipLabel(String file) {
        return file.replaceFirst("(?i)\\.zip$", "").replaceFirst("^VanillaTweaks_", "");
    }

    private List<PackEntry> findInstalled(Set<String> ids) {
        List<PackEntry> found = new ArrayList<>();
        Set<String> added = new HashSet<>();
        for (Category category : this.categories) {
            this.collectInstalled(category, ids, found, added);
        }
        return found;
    }

    private void collectInstalled(Category category, Set<String> ids, List<PackEntry> found, Set<String> added) {
        for (AbstractEntry entry : this.getPackEntries(category)) {
            if (entry instanceof PackEntry pack && ids.contains(pack.pack.getId()) && added.add(pack.pack.getId())) {
                found.add(pack);
            }
        }

        if (category.getSubCategories() != null) {
            for (Category.SubCategory subCategory : category.getSubCategories()) {
                this.collectInstalled(subCategory, ids, found, added);
            }
        }
    }

    private void setInstalledFilter(@Nullable String file) {
        if (!Objects.equals(file, this.installedFilter)) {
            this.installedFilter = file;
            this.refreshEntries();
            this.setScrollAmount(0.0);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (this.details != null) {
            return this.details.mouseScrolled(verticalAmount);
        }

        // The wheel over the chips scrolls them sideways, when they don't all fit. So does a sideways scroll anywhere
        // in the list: shift and the wheel (macOS turns that into a sideways scroll itself), or a trackpad swipe.
        if (this.installedChips != null && this.children().contains(this.installedChips)) {
            // A trackpad's vertical swipe drifts sideways a little, so only the larger direction counts
            boolean mostlySideways = Math.abs(horizontalAmount) > Math.abs(verticalAmount);
            if ((mostlySideways || Minecraft.getInstance().hasShiftDown() || this.installedChips.isMouseOver(mouseX, mouseY))
                    && this.installedChips.scrollBy(mostlySideways ? horizontalAmount : verticalAmount)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    // region details
    /** Opens a pack's page over the list. Back, Escape or a new category closes it. */
    private void openDetails(PackEntry entry) {
        this.closeDetails();
        this.cursorShown = false;
        List<String> incompatible = new ArrayList<>();
        for (String id : entry.getPack().getIncompatiblePacks()) {
            Pack pack = this.findPack(id);
            incompatible.add(pack != null ? pack.getName() : id);
        }
        this.details = new PackDetails(this, entry, incompatible);
        this.screen.onDetailsChanged();
    }

    public boolean isDetailsOpen() {
        return this.details != null;
    }

    @Nullable
    public Pack getDetailsPack() {
        return this.details != null ? this.details.getPack() : null;
    }

    public void closeDetails() {
        if (this.details != null) {
            this.details.close();
            this.details = null;
            this.screen.onDetailsChanged();
        }
    }

    void toggleFromDetails(PackEntry entry) {
        this.toggleSelection(entry);
        this.selectionAnchor = entry.pack;
        this.cursor = entry.pack;
    }

    /** Opens the page of the pack under the arrow-key cursor. */
    public boolean openCursorDetails() {
        if (!this.cursorShown || this.cursor == null || this.details != null) {
            return false;
        }
        for (PackEntry entry : this.getShownPacks()) {
            if (entry.pack == this.cursor) {
                this.openDetails(entry);
                return true;
            }
        }
        return false;
    }

    @Nullable
    private Pack findPack(String id) {
        for (Category category : this.categories) {
            Pack pack = findPack(category, id);
            if (pack != null) {
                return pack;
            }
        }
        return null;
    }

    @Nullable
    private static Pack findPack(Category category, String id) {
        for (Pack pack : category.getPacks()) {
            if (pack.getId().equals(id)) {
                return pack;
            }
        }
        if (category.getSubCategories() != null) {
            for (Category.SubCategory subCategory : category.getSubCategories()) {
                Pack pack = findPack(subCategory, id);
                if (pack != null) {
                    return pack;
                }
            }
        }
        return null;
    }

    private static int countPacks(Category category) {
        return (int) category.getPacks().stream().filter(pack -> !pack.isExperimental()).count();
    }
    // endregion

    public void updateSelection() {
        for (List<AbstractEntry> categoryEntries : this.entryCache.values()) {
            for (AbstractEntry entry : categoryEntries) {
                if (entry instanceof PackEntry packEntry) {
                    if (this.selectionHelper.isSelected(packEntry.pack) != packEntry.selectionData.isSelected()) {
                        packEntry.selectionData.toggleSelection();
                    }
                }
            }
        }
    }

    private void toggleSelection(PackEntry entry) {
        if (this.editable) {
            this.selectionHelper.toggleSelection(entry);
        }
    }

    /** A click on a pack. With shift, every pack from the last one clicked takes on that pack's state. */
    private void clickPack(PackEntry entry, boolean shift) {
        if (shift && this.selectionAnchor != null && this.editable) {
            List<PackEntry> shown = this.getShownPacks();
            int from = this.indexOfPack(shown, this.selectionAnchor);
            int to = this.indexOfPack(shown, entry.pack);
            if (from != -1 && to != -1) {
                boolean selected = this.selectionHelper.isSelected(this.selectionAnchor);
                for (PackEntry e : shown.subList(Math.min(from, to), Math.max(from, to) + 1)) {
                    this.setPackSelected(e, selected);
                }
                return;
            }
        }

        this.toggleSelection(entry);
        this.selectionAnchor = entry.pack;
        this.cursor = entry.pack;
    }

    private void setPackSelected(PackEntry entry, boolean selected) {
        if (this.selectionHelper.isSelected(entry.pack) != selected) {
            this.toggleSelection(entry);
        }
    }

    private int indexOfPack(List<PackEntry> entries, Pack pack) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).pack == pack) {
                return i;
            }
        }

        return -1;
    }

    /** The packs in this list, in order, in either layout. */
    private List<PackEntry> getShownPacks() {
        List<PackEntry> packs = new ArrayList<>();
        for (AbstractEntry entry : this.children()) {
            if (entry instanceof PackEntry pack) {
                packs.add(pack);
            } else if (entry instanceof TileRowEntry row) {
                packs.addAll(row.tiles);
            }
        }

        return packs;
    }

    /**
     * Moves the arrow-key cursor. In the grid, up and down move a row; in the list, left and right do nothing.
     * The first press shows the cursor where it was, or on the first pack in view.
     */
    public boolean moveCursor(int dx, int dy) {
        if (this.details != null) {
            // In the list, left leaves a pack's page as right opened it
            if (dx < 0 && this.layout == PackLayout.LIST) {
                this.closeDetails();
                return true;
            }
            return false;
        }
        if (dx > 0 && this.layout == PackLayout.LIST) {
            return this.openCursorDetails();
        }

        List<PackEntry> shown = this.getShownPacks();
        if (shown.isEmpty()) {
            return false;
        }

        int index = this.cursor != null ? this.indexOfPack(shown, this.cursor) : -1;
        if (index == -1 || !this.cursorShown) {
            index = index != -1 ? index : this.firstPackInView(shown);
        } else if (this.layout == PackLayout.GRID) {
            index = Math.clamp(index + dx + dy * this.tileColumns, 0, shown.size() - 1);
        } else if (dy != 0) {
            index = Math.clamp(index + dy, 0, shown.size() - 1);
        }

        this.cursor = shown.get(index).pack;
        this.cursorShown = true;
        this.scrollToCursor();
        return true;
    }

    private int firstPackInView(List<PackEntry> shown) {
        for (AbstractEntry entry : this.children()) {
            if (entry.getY() >= this.getY() && entry instanceof PackEntry pack) {
                return this.indexOfPack(shown, pack.pack);
            } else if (entry.getY() >= this.getY() && entry instanceof TileRowEntry row) {
                return this.indexOfPack(shown, row.tiles.getFirst().pack);
            }
        }

        return 0;
    }

    private void scrollToCursor() {
        for (AbstractEntry entry : this.children()) {
            if ((entry instanceof PackEntry pack && pack.pack == this.cursor)
                    || (entry instanceof TileRowEntry row && row.contains(this.cursor))) {
                this.scrollToEntry(entry);
                return;
            }
        }
    }

    /** Space or Enter: selects or deselects the pack the cursor is on, like a click. */
    public boolean activateCursor(boolean shift) {
        if (this.details != null) {
            this.toggleFromDetails(this.details.entry);
            return true;
        }
        if (!this.cursorShown || this.cursor == null) {
            return false;
        }

        for (PackEntry entry : this.getShownPacks()) {
            if (entry.pack == this.cursor) {
                this.clickPack(entry, shift);
                return true;
            }
        }

        return false;
    }

    public void hideCursor() {
        this.cursorShown = false;
    }

    private boolean isCursorOn(PackEntry entry) {
        return this.cursorShown && entry.pack == this.cursor;
    }

    private static void outline(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1) {
        graphics.fill(x0, y0, x1, y0 + 1, Theme.WHITE);
        graphics.fill(x0, y1 - 1, x1, y1, Theme.WHITE);
        graphics.fill(x0, y0, x0 + 1, y1, Theme.WHITE);
        graphics.fill(x1 - 1, y0, x1, y1, Theme.WHITE);
    }

    /** Selects every pack in this list, or deselects them if they're all selected already. */
    public void selectAllShown() {
        if (!this.editable || this.details != null) {
            return;
        }

        List<PackEntry> shown = this.getShownPacks();
        boolean selected = !shown.stream().allMatch(e -> this.selectionHelper.isSelected(e.pack));
        for (PackEntry entry : shown) {
            this.setPackSelected(entry, selected);
        }
        this.selectionAnchor = null;
    }

    public boolean isEditable() {
        return this.editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    private int getEntrySelectionColor(AbstractEntry entry) {
        if (entry instanceof PackEntry packEntry) {
            return this.selectionHelper.getSelectionColor(packEntry.getPack());
        }

        return PackSelectionHelper.DEFAULT_SELECTION_COLOR;
    }

    private int getCenterX() {
        return this.getX() + this.width / 2;
    }

    private int getCenterY() {
        return this.getY() + this.height / 2;
    }

    private static int getLineHeight(Font textRenderer) {
        return textRenderer.lineHeight + TEXT_MARGIN;
    }

    public void focusPack(Pack pack) {
        AbstractEntry entry = null;
        for (int i = 0; i < this.children().size(); i++) {
            AbstractEntry e = this.children().get(i);
            if (e instanceof PackEntry packEntry && packEntry.pack == pack) {
                entry = packEntry;
            } else if (e instanceof TileRowEntry row && row.contains(pack)) {
                entry = row;
            }
        }

        if (entry != null) {
            this.centerScrollOn(entry);
        }
    }

    @Override
    public int getRowWidth() {
        return this.width - ROW_LEFT_RIGHT_MARGIN * 2;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - SCROLLBAR_WIDTH;
    }

    private int getTooltipWidth() {
        // From the screen, as the list is too narrow for long descriptions to fit on the screen's height
        return (this.screen.width / 5) * 2;
    }

    // private void moveFocus(MoveDirection direction) {
    //     int offset = direction == MoveDirection.UP ? -1 : 1;
    //     if (!this.children().isEmpty()) {
    //         int start = this.children().get(0) instanceof WarningEntry ? 1 : 0;
    //         AbstractEntry current = this.getFocused();
    //         int currentIndex = current != null ? this.children().indexOf(current) : -1;
    //
    //         int index = MathHelper.clamp(currentIndex + offset, start, this.getEntryCount() - 1);
    //         if (index != currentIndex) {
    //             AbstractEntry entry = this.getEntry(index);
    //             this.setFocused(entry);
    //             this.ensureVisible(entry);
    //         }
    //     }
    // }

    @Override
    public boolean isFocused() {
        return this.screen.getFocused() == this;
    }

    // @Override
    // public void setFocused(@Nullable Element focused) {
    //     super.setFocused(focused);
    //
    //     // Set focused element as list when focusing an entry
    //     if (focused != null && !this.isFocused()) {
    //         this.screen.setFocused(this);
    //     }
    // }

    /** The download error, shown when a category has nothing in it because the pack list didn't load. */
    private boolean isErrorShown() {
        return this.children().isEmpty() && this.searchTerms == null
                && (this.category == null || this.category != this.installedCategory);
    }

    void visitLines(ActiveTextCollector textCollector) {
        if (!this.isErrorShown()) {
            return;
        }

        // visit error lines
        Font textRenderer = this.minecraft.font;

        int x = this.getCenterX();
        int y = this.getCenterY();
        int lineHeight = getLineHeight(textRenderer);

        int textY = y - lineHeight * 2;
        TextAlignment alignment = TextAlignment.CENTER;
        this.errorText.visitLines(alignment, x, textY, lineHeight, textCollector);
    }

    // region input callbacks
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        this.cursorShown = false;
        if (this.details != null) {
            boolean inside = event.x() >= this.getX() && event.x() < this.getRight()
                    && event.y() >= this.getY() && event.y() < this.getBottom();
            if (inside && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                this.details.mouseClicked(event.x(), event.y());
            }
            return inside;
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.isErrorShown()) {
            // Handle clicks when the error is shown
            double mouseX = event.x();
            double mouseY = event.y();
            if (mouseX >= this.getX() && mouseX < this.getRight()
                    && mouseY >= this.getY() && mouseY < this.getBottom()) {
                Font textRenderer = this.minecraft.font;

                ActiveTextCollector.ClickableStyleFinder styleFinder = new ActiveTextCollector.ClickableStyleFinder(textRenderer, (int) mouseX, (int) mouseY);
                this.visitLines(styleFinder);
                Style style = styleFinder.result();

                if (style != null && style.getClickEvent() != null
                        && style.getClickEvent().action() == ClickEvent.Action.OPEN_URL) {
                    URI uri = ((ClickEvent.OpenUrl) style.getClickEvent()).uri();
                    Util.openUri(this.minecraft, this.screen, uri);

                    return true;
                }
            }
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isFocused()) {
            // if (keyCode == GLFW.GLFW_KEY_DOWN) {
            //     this.moveFocus(MoveDirection.DOWN);
            //     return true;
            // } else if (keyCode == GLFW.GLFW_KEY_UP) {
            //     this.moveFocus(MoveDirection.UP);
            //     return true;
            // }

            if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
                AbstractEntry focusedEntry = this.getFocused();
                if (focusedEntry instanceof PackEntry entry) {
                    this.toggleSelection(entry);
                }
            }
        }

        return false;
    }

    // @Override
    // public boolean changeFocus(boolean lookForwards) {
    //     return !this.isFocused();
    // }

    // endregion

    // region render
    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (this.details != null) {
            boolean covered = this.screen.isCoveredByPopup(mouseX, mouseY);
            this.details.render(graphics, this.getX(), this.getY(), this.width, this.height,
                    covered ? -1 : mouseX, covered ? -1 : mouseY);
            return;
        }

        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);

        if (this.isErrorShown()) {
            this.renderError(graphics);
        } else if (this.children().isEmpty()) {
            Component text = this.searchTerms != null ? NO_RESULTS_TEXT : NO_INSTALLED_TEXT;
            Font font = this.minecraft.font;
            graphics.text(font, text.getString(), this.getCenterX() - font.width(text) / 2,
                    this.getCenterY(), Theme.FAINT, false);
        }
    }

    @Override
    protected void extractListBackground(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractListSeparators(GuiGraphicsExtractor graphics) {
    }

    @Override
    protected void extractScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.scrollable()) {
            Theme.scrollThumb(graphics, this.getRight(), this.scrollBarY(), this.scrollerHeight(),
                    this.isOverScrollbar(mouseX, mouseY));
        }
    }

    @Override
    protected void extractItem(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, AbstractEntry entry) {
        boolean hovered = Objects.equals(this.getHovered(), entry) && !this.screen.isCoveredByPopup(mouseX, mouseY);

        if (entry instanceof PackEntry packEntry) {
            if (packEntry.selectionData.isSelected()) {
                this.drawEntrySelectionHighlight(graphics, entry, this.getAccentColor(packEntry), hovered);
            } else if (hovered) {
                graphics.fill(entry.getX(), entry.getY(), entry.getX() + entry.getWidth(),
                        entry.getY() + entry.getHeight() - 1, Theme.ROW_HOVER);
            }
        }

        entry.extractContent(graphics, mouseX, mouseY, hovered, delta);

        if (entry instanceof PackEntry packEntry && this.isCursorOn(packEntry)) {
            outline(graphics, entry.getX(), entry.getY(), entry.getX() + entry.getWidth(), entry.getY() + entry.getHeight() - 1);
        }
    }

    /** Green for a plain selection, or the colour that links the pack with the ones it's incompatible with. */
    private int getAccentColor(AbstractEntry entry) {
        int color = this.getEntrySelectionColor(entry);
        return color == PackSelectionHelper.DEFAULT_SELECTION_COLOR ? Theme.ACCENT : color;
    }

    private void drawEntrySelectionHighlight(GuiGraphicsExtractor graphics, AbstractEntry entry, int accent, boolean hovered) {
        int x1 = entry.getX();
        int y1 = entry.getY();
        int x2 = x1 + entry.getWidth();
        int y2 = y1 + entry.getHeight() - 1;
        float tint = hovered ? SELECTED_HOVER_TINT : SELECTED_TINT;
        graphics.fill(x1, y1, x2, y2, Theme.mix(Theme.ROW_TINT_BASE, accent, tint));
        graphics.fill(x1, y1, x1 + ACCENT_WIDTH, y2, accent);
    }

    private void renderError(GuiGraphicsExtractor graphics) {
        this.visitLines(graphics.textRenderer());
    }

    public void renderDebugInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!SHOW_DEBUG_INFO) return;
        Font textRenderer = this.minecraft.font;

        boolean hasCategory = this.category != null;
        List<String> debugInfo = List.of(
                "WxH = " + this.width + "x" + this.height,
                "EW = " + this.getRowWidth(),
                "C = " + (hasCategory ? this.category.getName() : "null"),
                "HI = " + (hasCategory ? this.category.isHardIncompatible() : "N/A"),
                "S = " + this.selectionHelper.getSelection(),
                "F = " + this.getFocused(),
                "E = " + this.editable,
                "IC = " + this.selectionHelper.usedColors,
                "MX/MY = " + mouseX + "/" + mouseY
        );

        RenderUtil.renderDebugInfo(graphics, textRenderer, this.getX(), this.getBottom(), debugInfo);
    }

    public void renderTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.details == null && mouseY >= this.getY() && mouseY < this.getBottom()
                && mouseX >= this.getX() && mouseX < this.getRight()
                && !this.screen.isCoveredByPopup(mouseX, mouseY)) {
            int width = this.getTooltipWidth();
            for (AbstractEntry entry : this.children()) {
                if (entry.renderTooltip(graphics, mouseX, mouseY, width)) {
                    break;
                }
            }
        }
    }
    // endregion

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, Constants.RESOURCE_PACK_SCREEN_SUBTITLE);
    }

    public static class PackEntry extends AbstractEntry {
        private final Pack pack;
        private final Component name;

        private final PackSelectionListWidget widget;
        private final Identifier icon;
        private boolean downloadedIcon = false;
        private boolean iconExists;

        private NativeImage downloadedIconImage;

        private List<Component> description;
        private int descriptionWidth = -1;
        private String searchText;

        protected PackSelectionData selectionData;

        public PackEntry(PackSelectionListWidget widget, Pack pack, Category category) {
            super(widget);
            this.pack = pack;
            this.name = Component.nullToEmpty(pack.getName());
            this.widget = widget;

            this.icon = VTDMod.getIconId(pack);

            this.iconExists = ((TextureManagerAccess) this.client.getTextureManager()).vtdownloader$hasTexture(this.icon);

            this.selectionData = new PackSelectionData(this.pack, category);
        }

        /** The start of the description: the rest is on the pack's page. */
        protected List<Component> getDescriptionLines(int maxWidth) {
            List<Component> lines = this.wrapEscapedText(this.pack.getDescription(), maxWidth);
            if (lines.size() <= DESCRIPTION_LINES) {
                return lines;
            }

            List<Component> shown = new ArrayList<>(lines.subList(0, DESCRIPTION_LINES - 1));
            String last = lines.get(DESCRIPTION_LINES - 1).getString() + " " + lines.get(DESCRIPTION_LINES).getString();
            shown.add(Component.literal(Theme.clip(this.client.font, last, maxWidth)));
            return shown;
        }

        Component getName() {
            return this.name;
        }

        private int getArrowX() {
            return this.getX() + this.getWidth() - ARROW_MARGIN - Theme.ARROW_BUTTON_SIZE;
        }

        private int getArrowY() {
            return this.getY() + (this.getHeight() - 1 - Theme.ARROW_BUTTON_SIZE) / 2;
        }

        /** The start of the description, wrapped to {@code maxWidth}. */
        private List<Component> getDescription(int maxWidth) {
            if (this.description == null || maxWidth != this.descriptionWidth) {
                this.description = this.getDescriptionLines(maxWidth);
                this.descriptionWidth = maxWidth;
            }

            return this.description;
        }

        private static int getTextWidth(int rowWidth) {
            return rowWidth - ICON_LEFT - ICON_SIZE - ICON_RIGHT - ARROW_MARGIN * 2 - Theme.ARROW_BUTTON_SIZE;
        }

        /** Tall enough for the name and the lines of description shown. */
        @Override
        protected int getRowHeight(int rowWidth) {
            int lineHeight = this.client.font.lineHeight;
            int textHeight = lineHeight + TEXT_MARGIN + this.getDescription(getTextWidth(rowWidth)).size() * lineHeight;
            return Math.max(ITEM_HEIGHT, textHeight + ROW_PADDING * 2);
        }

        public Pack getPack() {
            return this.pack;
        }

        private boolean matches(List<String> terms) {
            if (this.searchText == null) {
                this.searchText = (this.name.getString() + " " + Util.removeHtmlTags(this.pack.getDescription())).toLowerCase(Locale.ROOT);
            }

            return terms.stream().allMatch(this.searchText::contains);
        }

        private void downloadIcon() {
            if (this.downloadedIconImage != null) {
                TextureManager textureManager = this.client.getTextureManager();
                DynamicTexture iconTexture = new DynamicTexture(this.pack::getId, this.downloadedIconImage);
                textureManager.register(this.icon, iconTexture);
                this.iconExists = true;
                this.downloadedIconImage = null;
            }

            if (this.downloadedIcon || this.iconExists) return;

            this.downloadedIcon = true;

            mix.vtdrefresh.data.IconCache.get(this.pack).whenCompleteAsync((icon, throwable) -> {
                if (throwable != null) {
                    VTDMod.LOGGER.error("Failed to download icon for pack {}", this.pack.getName(), throwable);
                    return;
                }

                if (icon != null) {
                    this.downloadedIconImage = icon;
                } else {
                    VTDMod.LOGGER.error("Failed to download icon for pack {}", this.pack.getName());
                }
            });
        }

        @Override
        protected List<Component> getTooltipText(int width) {
            // The row shows the whole description
            return Collections.emptyList();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                if (Theme.isOver(event.x(), event.y(), this.getArrowX(), this.getArrowY(), Theme.ARROW_BUTTON_SIZE)) {
                    this.widget.openDetails(this);
                } else {
                    this.widget.clickPack(this, event.hasShiftDown());
                }
                return true;
            }

            return false;
        }

        // region entryRender
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getX();
            int y = this.getY();
            int height = this.getHeight() - 1;
            int width = this.getWidth();
            Font textRenderer = this.client.font;

            int iconX = x + ICON_LEFT;
            int iconY = y + (height - ICON_SIZE) / 2;
            if (!DISABLE_ICONS) this.renderIcon(graphics, iconX, iconY, ICON_SIZE);

            int textX = iconX + ICON_SIZE + ICON_RIGHT;
            int textWidth = getTextWidth(width);
            List<Component> description = this.getDescription(textWidth);

            int lineHeight = textRenderer.lineHeight;
            int totalHeight = lineHeight + TEXT_MARGIN + description.size() * lineHeight;
            int textY = y + (height - totalHeight) / 2;

            boolean plainSelection = this.selectionData.isSelected()
                    && this.widget.getEntrySelectionColor(this) == PackSelectionHelper.DEFAULT_SELECTION_COLOR;
            String title = Theme.clip(textRenderer, this.name.getString(), textWidth);
            graphics.text(textRenderer, title, textX, textY, plainSelection ? Theme.ACCENT : Theme.WHITE, false);

            int descriptionY = textY + lineHeight + TEXT_MARGIN;
            for (Component line : description) {
                graphics.text(textRenderer, line, textX, descriptionY, Theme.DIM, false);
                descriptionY += lineHeight;
            }

            Theme.arrowButton(graphics, this.getArrowX(), this.getArrowY(), hovered,
                    hovered && Theme.isOver(mouseX, mouseY, this.getArrowX(), this.getArrowY(), Theme.ARROW_BUTTON_SIZE));
        }

        void renderIcon(GuiGraphicsExtractor graphics, int x, int y, int size) {
            downloadIcon();
            if (!this.iconExists) {
                graphics.fill(x, y, x + size, y + size, Theme.ROW_HOVER);
                return;
            }

            graphics.blit(RenderPipelines.GUI_TEXTURED, this.icon, x, y, 0.0F, 0.0F, size, size, size, size);
        }
        // endregion

        @Override
        public String toString() {
            return "Pack " + this.pack.getName();
        }
    }

    /** A row of the grid layout, showing some of the category's packs as tiles. */
    public static class TileRowEntry extends AbstractEntry {
        private final PackSelectionListWidget widget;
        private final List<PackEntry> tiles;
        private final int columns;

        protected TileRowEntry(PackSelectionListWidget widget, List<PackEntry> tiles, int columns) {
            super(widget);
            this.widget = widget;
            this.tiles = tiles;
            this.columns = columns;
        }

        private boolean contains(Pack pack) {
            return this.tiles.stream().anyMatch(tile -> tile.pack == pack);
        }

        private int getTileWidth() {
            return (this.getWidth() - TILE_GAP * (this.columns - 1)) / this.columns;
        }

        private int getTileHeight() {
            return this.getHeight() - TILE_GAP;
        }

        private int getTileLeft(int index) {
            return this.getX() + index * (this.getTileWidth() + TILE_GAP);
        }

        @org.jetbrains.annotations.Nullable
        private PackEntry getTileAt(double mouseX, double mouseY) {
            if (mouseY < this.getY() || mouseY >= this.getY() + this.getTileHeight()) {
                return null;
            }

            for (int i = 0; i < this.tiles.size(); i++) {
                int left = this.getTileLeft(i);
                if (mouseX >= left && mouseX < left + this.getTileWidth()) {
                    return this.tiles.get(i);
                }
            }

            return null;
        }

        @Override
        protected List<Component> getTooltipText(int width) {
            return Collections.emptyList();
        }

        @Override
        protected boolean renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int width) {
            PackEntry tile = this.getTileAt(mouseX, mouseY);
            if (tile == null) {
                return false;
            }

            // A name cut short is shown whole; the description is on the pack's page
            Font font = this.client.font;
            if (font.width(tile.name) > this.getTileWidth() - TILE_NAME_MARGIN * 2) {
                graphics.setComponentTooltipForNextFrame(font, List.of(tile.name), mouseX, mouseY);
            }
            return true;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            PackEntry tile = this.getTileAt(event.x(), event.y());
            if (tile != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                int index = this.tiles.indexOf(tile);
                if (this.isOverArrow(index, event.x(), event.y())) {
                    this.widget.openDetails(tile);
                } else {
                    this.widget.clickPack(tile, event.hasShiftDown());
                }
                return true;
            }

            return false;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            PackEntry hoveredTile = hovered ? this.getTileAt(mouseX, mouseY) : null;
            for (int i = 0; i < this.tiles.size(); i++) {
                PackEntry tile = this.tiles.get(i);
                boolean tileHovered = tile == hoveredTile;
                this.renderTile(graphics, tile, this.getTileLeft(i), tileHovered);
                if (tileHovered) {
                    Theme.arrowButton(graphics, this.getArrowX(i), this.getArrowY(), true,
                            this.isOverArrow(i, mouseX, mouseY));
                }
            }
        }

        private int getArrowX(int index) {
            return this.getTileLeft(index) + this.getTileWidth() - TILE_ARROW_INSET - Theme.ARROW_BUTTON_SIZE;
        }

        private int getArrowY() {
            return this.getY() + TILE_ARROW_INSET;
        }

        private boolean isOverArrow(int index, double mouseX, double mouseY) {
            return Theme.isOver(mouseX, mouseY, this.getArrowX(index), this.getArrowY(), Theme.ARROW_BUTTON_SIZE);
        }

        private void renderTile(GuiGraphicsExtractor graphics, PackEntry tile, int x, boolean hovered) {
            int y = this.getY();
            int width = this.getTileWidth();
            int bottom = y + this.getTileHeight();
            boolean selected = tile.selectionData.isSelected();
            int accent = this.widget.getAccentColor(tile);

            if (selected) {
                float tint = hovered ? SELECTED_HOVER_TINT : SELECTED_TINT;
                graphics.fill(x, y, x + width, bottom, Theme.mix(Theme.ROW_TINT_BASE, accent, tint));
                graphics.fill(x, y, x + ACCENT_WIDTH, bottom, accent);
            } else if (hovered) {
                graphics.fill(x, y, x + width, bottom, Theme.ROW_HOVER);
            }

            if (!DISABLE_ICONS) tile.renderIcon(graphics, x + (width - ICON_SIZE) / 2, y + TILE_TOP, ICON_SIZE);

            Font font = this.client.font;
            boolean plainSelection = selected && this.widget.getEntrySelectionColor(tile) == PackSelectionHelper.DEFAULT_SELECTION_COLOR;
            if (this.widget.isCursorOn(tile)) {
                outline(graphics, x, y, x + width, bottom);
            }

            String name = Theme.clip(font, tile.name.getString(), width - TILE_NAME_MARGIN * 2);
            graphics.text(font, name, x + (width - font.width(name)) / 2, y + TILE_NAME_TOP,
                    plainSelection ? Theme.ACCENT : Theme.WHITE, false);
        }

        @Override
        public String toString() {
            return "Tile row " + this.tiles;
        }
    }

    /** A category's note, like a warning about packs that need a mod. Shows the whole text. */
    public static class WarningEntry extends AbstractEntry {
        private final Category.Warning warning;
        private final int color;

        private List<Component> lines;
        private int linesWidth = -1;

        public WarningEntry(PackSelectionListWidget widget, Category.Warning warning) {
            super(widget);
            this.warning = warning;

            this.color = mix.vtdrefresh.gui.Colors.parse(warning.getColor());
        }

        private static int getTextWidth(int rowWidth) {
            return rowWidth - WARNING_MARGIN * 2;
        }

        private List<Component> getLines(int maxWidth) {
            if (this.lines == null || maxWidth != this.linesWidth) {
                this.lines = this.wrapEscapedText(this.warning.getText(), maxWidth);
                this.linesWidth = maxWidth;
            }

            return this.lines;
        }

        @Override
        protected int getRowHeight(int rowWidth) {
            int textHeight = this.getLines(getTextWidth(rowWidth)).size() * this.client.font.lineHeight;
            return textHeight + (WARNING_PADDING + WARNING_BG_MARGIN) * 2;
        }

        @Override
        protected List<Component> getTooltipText(int width) {
            return Collections.emptyList();
        }

        // region warningRender
        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getX();
            int top = this.getY() + WARNING_BG_MARGIN;
            int bottom = this.getY() + this.getHeight() - WARNING_BG_MARGIN;

            // The colour marks the note without filling it, so the text stays easy to read
            graphics.fill(x, top, x + this.getWidth(), bottom, Theme.ROW_TINT_BASE);
            graphics.fill(x, top, x + ACCENT_WIDTH, bottom, Theme.withAlpha(this.color, 0xFF));

            int lineHeight = this.client.font.lineHeight;
            int y = top + WARNING_PADDING;
            for (Component line : this.getLines(getTextWidth(this.getWidth()))) {
                graphics.text(this.client.font, line, x + WARNING_MARGIN, y, Theme.DIM, false);
                y += lineHeight;
            }
        }
        // endregion

        @Override
        public String toString() {
            return "Warning";
        }
    }

    /** A subcategory, after its category's packs: a row in the list, opening it with a click. */
    public static class SubCategoryEntry extends AbstractEntry {
        private final Category.SubCategory category;
        private final Component name;
        private final String count;

        public SubCategoryEntry(PackSelectionListWidget widget, Category.SubCategory category) {
            super(widget);
            this.category = category;
            this.name = Component.literal(category.getName());
            this.count = Component.translatable("vtd.subcategory.count", countPacks(category)).getString();
        }

        @Override
        protected int getRowHeight(int rowWidth) {
            return SUBCATEGORY_ROW_HEIGHT;
        }

        @Override
        protected List<Component> getTooltipText(int width) {
            return Collections.emptyList();
        }

        void open() {
            this.screen.selectCategory(this.category);
        }

        /** The first few of its packs' icons, side by side, so a subcategory shows what's inside. */
        void renderIcons(GuiGraphicsExtractor graphics, int x, int y, int max) {
            int drawn = 0;
            for (AbstractEntry entry : this.parentWidget.getPackEntries(this.category)) {
                if (entry instanceof PackEntry pack && drawn < max) {
                    pack.renderIcon(graphics, x + drawn * (SUBCATEGORY_ICON_SIZE + 2), y, SUBCATEGORY_ICON_SIZE);
                    drawn++;
                }
            }
        }

        static int iconsWidth(int count) {
            return count * (SUBCATEGORY_ICON_SIZE + 2) - 2;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                this.open();
                return true;
            }
            return false;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getX();
            int y = this.getY() + 2;
            int bottom = this.getY() + this.getHeight() - 2;
            int right = x + this.getWidth();
            Theme.box(graphics, x, y, right, bottom, hovered ? Theme.ROW_HOVER : Theme.ROW_TINT_BASE);

            Font font = this.client.font;
            int arrowX = right - ARROW_MARGIN - Theme.ARROW_BUTTON_SIZE;
            // Fewer icons before the name gets cut short
            int textRoom = font.width(this.name) + 6 + font.width(this.count) + ARROW_MARGIN;
            int iconCount = Math.min(3, countPacks(this.category));
            while (iconCount > 1 && x + ICON_LEFT + iconsWidth(iconCount) + ICON_RIGHT + textRoom > arrowX) {
                iconCount--;
            }
            int iconY = y + (bottom - y - SUBCATEGORY_ICON_SIZE) / 2;
            this.renderIcons(graphics, x + ICON_LEFT, iconY, iconCount);

            int textX = x + ICON_LEFT + iconsWidth(iconCount) + ICON_RIGHT;
            int textY = y + (bottom - y - 8) / 2;
            int countWidth = font.width(this.count);
            String name = Theme.clip(font, this.name.getString(), arrowX - ARROW_MARGIN - countWidth - 6 - textX);
            graphics.text(font, name, textX, textY, hovered ? Theme.WHITE : Theme.DIM, false);
            graphics.text(font, this.count, textX + font.width(name) + 6, textY, Theme.FAINT, false);

            int arrowY = y + (bottom - y - Theme.ARROW_BUTTON_SIZE) / 2;
            Theme.arrowButton(graphics, arrowX, arrowY, hovered,
                    hovered && Theme.isOver(mouseX, mouseY, arrowX, arrowY, Theme.ARROW_BUTTON_SIZE));
        }

        @Override
        public String toString() {
            return "Subcategory " + this.category.getName();
        }
    }

    /** Subcategories in the grid: boxes the size of a pack's tile, a row of them. */
    public static class SubCategoryRowEntry extends AbstractEntry {
        private final List<SubCategoryEntry> boxes;
        private final int columns;

        protected SubCategoryRowEntry(PackSelectionListWidget widget, List<SubCategoryEntry> boxes, int columns) {
            super(widget);
            this.boxes = boxes;
            this.columns = columns;
        }

        private int getBoxWidth() {
            return (this.getWidth() - TILE_GAP * (this.columns - 1)) / this.columns;
        }

        private int getBoxLeft(int index) {
            return this.getX() + index * (this.getBoxWidth() + TILE_GAP);
        }

        @Nullable
        private SubCategoryEntry getBoxAt(double mouseX, double mouseY) {
            if (mouseY < this.getY() || mouseY >= this.getY() + this.getHeight() - TILE_GAP) {
                return null;
            }
            for (int i = 0; i < this.boxes.size(); i++) {
                int left = this.getBoxLeft(i);
                if (mouseX >= left && mouseX < left + this.getBoxWidth()) {
                    return this.boxes.get(i);
                }
            }
            return null;
        }

        @Override
        protected List<Component> getTooltipText(int width) {
            return Collections.emptyList();
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            SubCategoryEntry box = this.getBoxAt(event.x(), event.y());
            if (box != null && event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                box.open();
                return true;
            }
            return false;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            SubCategoryEntry hoveredBox = hovered ? this.getBoxAt(mouseX, mouseY) : null;
            Font font = this.client.font;
            int y = this.getY();
            int bottom = y + this.getHeight() - TILE_GAP;
            int width = this.getBoxWidth();

            for (int i = 0; i < this.boxes.size(); i++) {
                SubCategoryEntry box = this.boxes.get(i);
                int x = this.getBoxLeft(i);
                boolean boxHovered = box == hoveredBox;
                Theme.box(graphics, x, y, x + width, bottom, boxHovered ? Theme.CONTROL_HOVER : Theme.CONTROL);

                int icons = Math.max(1, Math.min(3, Math.min(countPacks(box.category),
                        (width - 8) / (SUBCATEGORY_ICON_SIZE + 2))));
                box.renderIcons(graphics, x + (width - SubCategoryEntry.iconsWidth(icons)) / 2, y + 10, icons);

                String name = Theme.clip(font, box.name.getString(), width - TILE_NAME_MARGIN * 2);
                graphics.text(font, name, x + (width - font.width(name)) / 2, y + 30,
                        boxHovered ? Theme.WHITE : Theme.DIM, false);
                graphics.text(font, box.count, x + (width - font.width(box.count)) / 2, y + 41, Theme.FAINT, false);

                int arrowX = x + width - TILE_ARROW_INSET - Theme.ARROW_BUTTON_SIZE;
                if (boxHovered) {
                    Theme.arrowButton(graphics, arrowX, y + TILE_ARROW_INSET, true,
                            Theme.isOver(mouseX, mouseY, arrowX, y + TILE_ARROW_INSET, Theme.ARROW_BUTTON_SIZE));
                }
            }
        }

        @Override
        public String toString() {
            return "Subcategory boxes " + this.boxes;
        }
    }

    /** A zip in the Installed view's chip row. A null file is All. */
    private record Chip(@Nullable String file, String label, int count) {
    }

    /** The Installed view's chips: one per Vanilla Tweaks zip, and All when there's more than one. Scrolls sideways. */
    public static class ChipRowEntry extends AbstractEntry {
        private static final int CHIP_HEIGHT = 16;
        private static final int CHIP_PADDING = 6;
        private static final int CHIP_GAP = 4;
        private static final int CHIP_MAX_WIDTH = 150;
        private static final int ROW_HEIGHT = CHIP_HEIGHT + 10;
        private static final int SCROLL_STEP = 24;

        private final PackSelectionListWidget widget;
        private final List<Chip> chips;
        private int scrollX;

        private ChipRowEntry(PackSelectionListWidget widget, List<Chip> chips) {
            super(widget);
            this.widget = widget;
            this.chips = chips;
        }

        @Override
        protected int getRowHeight(int rowWidth) {
            return ROW_HEIGHT;
        }

        private String text(Chip chip) {
            return chip.label() + " " + chip.count();
        }

        private int chipWidth(Chip chip) {
            return Math.min(CHIP_MAX_WIDTH, this.client.font.width(this.text(chip)) + CHIP_PADDING * 2);
        }

        private int contentWidth() {
            int width = -CHIP_GAP;
            for (Chip chip : this.chips) {
                width += this.chipWidth(chip) + CHIP_GAP;
            }
            return width;
        }

        private int maxScroll() {
            return Math.max(0, this.contentWidth() - this.getWidth());
        }

        /** Scrolls by wheel clicks; false when everything already fits, so the list scrolls instead. */
        private boolean scrollBy(double amount) {
            if (this.maxScroll() == 0) {
                return false;
            }
            this.scrollX = Math.clamp(this.scrollX - (int) Math.round(amount * SCROLL_STEP), 0, this.maxScroll());
            return true;
        }

        private int chipTop() {
            return this.getY() + (ROW_HEIGHT - CHIP_HEIGHT) / 2 - 1;
        }

        @Nullable
        private Chip chipAt(double mouseX, double mouseY) {
            if (mouseY < this.chipTop() || mouseY >= this.chipTop() + CHIP_HEIGHT
                    || mouseX < this.getX() || mouseX >= this.getX() + this.getWidth()) {
                return null;
            }
            int x = this.getX() - this.scrollX;
            for (Chip chip : this.chips) {
                int width = this.chipWidth(chip);
                if (mouseX >= x && mouseX < x + width) {
                    return chip;
                }
                x += width + CHIP_GAP;
            }
            return null;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            Chip chip = event.button() == InputConstants.MOUSE_BUTTON_LEFT ? this.chipAt(event.x(), event.y()) : null;
            if (chip == null) {
                return false;
            }
            this.widget.setInstalledFilter(chip.file());
            return true;
        }

        @Override
        protected List<Component> getTooltipText(int width) {
            return Collections.emptyList();
        }

        @Override
        protected boolean renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int width) {
            Chip chip = this.chipAt(mouseX, mouseY);
            if (chip == null) {
                return false;
            }
            // Only a chip that had to cut its name repeats it
            if (chip.file() != null && this.chipWidth(chip) == CHIP_MAX_WIDTH) {
                graphics.setTooltipForNextFrame(Component.literal(chip.file()), mouseX, mouseY);
            }
            return true;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            Font font = this.client.font;
            int left = this.getX();
            int right = left + this.getWidth();
            int top = this.chipTop();
            Chip hoveredChip = hovered ? this.chipAt(mouseX, mouseY) : null;

            graphics.enableScissor(left, this.getY(), right, this.getY() + this.getHeight());
            int x = left - this.scrollX;
            for (Chip chip : this.chips) {
                int width = this.chipWidth(chip);
                if (x + width > left && x < right) {
                    boolean chosen = Objects.equals(chip.file(), this.widget.installedFilter);
                    int fill = chosen ? Theme.ACCENT : chip == hoveredChip ? Theme.CONTROL_HOVER : Theme.CONTROL;
                    Theme.box(graphics, x, top, x + width, top + CHIP_HEIGHT, fill);

                    String count = " " + chip.count();
                    String label = Theme.clip(font, chip.label(), width - CHIP_PADDING * 2 - font.width(count));
                    int textY = top + (CHIP_HEIGHT - 8) / 2;
                    graphics.text(font, label, x + CHIP_PADDING, textY, chosen ? Theme.BLACK : Theme.WHITE, false);
                    graphics.text(font, count, x + CHIP_PADDING + font.width(label), textY, chosen ? Theme.BLACK : Theme.FAINT, false);
                }
                x += width + CHIP_GAP;
            }
            graphics.disableScissor();

            // A thin track under the chips shows there's more to either side
            int max = this.maxScroll();
            if (max > 0) {
                int trackY = top + CHIP_HEIGHT + 3;
                int width = this.getWidth();
                int thumb = Math.max(16, width * width / this.contentWidth());
                int thumbX = left + (width - thumb) * this.scrollX / max;
                graphics.fill(left, trackY, right, trackY + 1, Theme.ROW_HOVER);
                graphics.fill(thumbX, trackY, thumbX + thumb, trackY + 1, Theme.FAINT);
            }
        }

        @Override
        public String toString() {
            return "Chips " + this.chips;
        }
    }

    public static abstract class AbstractEntry extends AbstractSelectionList.Entry<AbstractEntry> {
        protected final PackSelectionListWidget parentWidget;
        protected final Minecraft client;
        protected final RefreshScreen screen;

        protected AbstractEntry(PackSelectionListWidget widget) {
            this.parentWidget = widget;
            this.client = widget.minecraft;
            this.screen = widget.screen;
        }

        protected final List<Component> wrapEscapedText(String text, int maxWidth) {
            return Util.wrapText(this.client.font, tidy(Util.removeHtmlTags(text)), maxWidth);
        }

        /** Removing a link's tags leaves the spaces around it, as in "OptiFine , or", and lines can start indented. */
        private static String tidy(String text) {
            return text.replaceAll(" +([,.!?;:)])", "$1").replaceAll("\\n[ \\t]+", "\n").strip();
        }

        /** The row's height in the list layout, for rows {@code rowWidth} wide. */
        protected int getRowHeight(int rowWidth) {
            return ITEM_HEIGHT;
        }

        protected abstract List<Component> getTooltipText(int width);

        // region baseEntryRender
        protected boolean renderTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int width) {
            if (this.isMouseOver(mouseX, mouseY)) {
                List<Component> lines = this.getTooltipText(width);
                if (!lines.isEmpty()) {
                    graphics.setComponentTooltipForNextFrame(this.client.font, lines, mouseX, mouseY);
                }
                return true;
            }

            return false;
        }
        // endregion
    }
}
