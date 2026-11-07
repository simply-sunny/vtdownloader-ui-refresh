package mix.vtdrefresh.gui;

import me.bymartrixx.vtd.VTDMod;
import me.bymartrixx.vtd.access.EntryBaseAccess;
import me.bymartrixx.vtd.data.Category;
import me.bymartrixx.vtd.data.DownloadPackRequestData;
import me.bymartrixx.vtd.data.Pack;
import me.bymartrixx.vtd.data.RpCategories;
import me.bymartrixx.vtd.data.SharePackRequestData;
import mix.vtdrefresh.data.InstalledPacks;
import mix.vtdrefresh.gui.popup.MessageScreenPopup;
import mix.vtdrefresh.gui.popup.ProgressBarScreenPopup;
import mix.vtdrefresh.gui.widget.CategorySelectionWidget;
import mix.vtdrefresh.gui.widget.DebugButtonWidget;
import mix.vtdrefresh.gui.widget.FlatButtonWidget;
import mix.vtdrefresh.gui.widget.FlatTextFieldWidget;
import mix.vtdrefresh.gui.widget.IconButtonWidget;
import mix.vtdrefresh.gui.widget.MutableMessageButtonWidget;
import mix.vtdrefresh.gui.widget.PackNameTextFieldWidget;
import mix.vtdrefresh.gui.widget.PackSelectionHelper;
import mix.vtdrefresh.gui.widget.PackSelectionListWidget;
import mix.vtdrefresh.gui.widget.ReloadButtonWidget;
import mix.vtdrefresh.gui.widget.SelectedPacksListWidget;
import me.bymartrixx.vtd.util.Constants;
import me.bymartrixx.vtd.util.RenderUtil;
import me.bymartrixx.vtd.util.Util;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RefreshScreen extends Screen {
    // DEBUG
    private static final boolean SHOW_DEBUG_INFO = false;
    private static final boolean DOWNLOAD_DISABLED = false;
    private static final boolean DEBUG_BUTTON = false;
    private static final boolean DEBUG_SHARE = false;
    private static final String DEBUG_SHARE_CODE = "abcdef";

    private static final Component TITLE = Component.literal("VTDownloader");
    private static final Component DOWNLOAD_TEXT = Component.translatable("vtd.download");
    private static final Component DOWNLOAD_FAILED_TEXT = Component.translatable("vtd.download.failed");
    private static final Component DOWNLOAD_SUCCESS_TEXT = Component.translatable("vtd.download.success");
    private static final Component SHARE_TEXT = Component.translatable("vtd.share");
    private static final Component SHARE_FAILED_TEXT = Component.translatable("vtd.share.failed");
    private static final Function<Component, Component> SHARE_CODE_TEXT = code -> Component.translatable("vtd.share.code", code);
    private static final Component READ_PACK_DATA_FAILED_TEXT = Component.translatable("vtd.readPackDataFailed");
    private static final Component PACK_NAME_FIELD_TEXT = Component.translatable("vtd.resourcePack.namePlaceholder");
    private static final Component PLACEHOLDER_TEXT = Component.literal("Lorem ipsum dolor sit amet");
    private static final Component SELECTED_PACKS_TEXT = Component.translatable("vtd.selectedPacks");
    private static final Component NOTHING_SELECTED_TEXT = Component.translatable("vtd.selectedPacks.empty");
    private static final Component BACK_TEXT = Component.translatable("vtd.nav.back");
    private static final Component FORWARD_TEXT = Component.translatable("vtd.nav.forward");
    private static final Component SHOW_SIDEBAR_TEXT = Component.translatable("vtd.panel.showCategories");
    private static final Component SHOW_SELECTED_PACKS_TEXT = Component.translatable("vtd.panel.showSelectedPacks");
    private static final Component LIST_LAYOUT_TEXT = Component.translatable("vtd.layout.list");
    private static final Component GRID_LAYOUT_TEXT = Component.translatable("vtd.layout.grid");

    private static final Component INSTALLED_TEXT = Component.translatable("vtd.installed");
    private static final Component SEARCH_PLACEHOLDER_TEXT = Component.translatable("vtd.search.placeholder");
    private static final Component SEARCH_BUTTON_TEXT = Component.translatable("vtd.search.button",
            System.getProperty("os.name").startsWith("Mac") ? "Cmd+F" : "Ctrl+F");

    private static final String[] SEARCH_ICON = {
            "..XXX....",
            ".X...X...",
            "X.....X..",
            "X.....X..",
            "X.....X..",
            ".X...X...",
            "..XXXX...",
            "......XX.",
            ".......XX"
    };
    private static final String[] LIST_ICON = {
            ".........",
            "X.XXXXXXX",
            ".........",
            ".........",
            "X.XXXXXXX",
            ".........",
            ".........",
            "X.XXXXXXX",
            "........."
    };
    private static final String[] GRID_ICON = {
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX",
            ".........",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX",
            "XXXX.XXXX"
    };

    private static final String[] BACK_ICON = {
            "....X",
            "...X.",
            "..X..",
            ".X...",
            "X....",
            ".X...",
            "..X..",
            "...X.",
            "....X"
    };
    private static final String[] FORWARD_ICON = {
            "X....",
            ".X...",
            "..X..",
            "...X.",
            "....X",
            "...X.",
            "..X..",
            ".X...",
            "X...."
    };
    private static final String[] SHOW_SIDEBAR_ICON = {
            "X..",
            ".X.",
            "..X",
            ".X.",
            "X.."
    };
    private static final String[] SHOW_SELECTED_PACKS_ICON = {
            "..X",
            ".X.",
            "X..",
            ".X.",
            "..X"
    };

    private static final int GAP = 4;
    private static final int BOTTOM_BAR_HEIGHT = 28;
    private static final int WIDGET_HEIGHT = 18;
    private static final int WIDGET_MARGIN = 4;
    private static final int PANEL_PADDING = 8;

    private static final int SIDEBAR_MIN_WIDTH = 112;
    private static final int SIDEBAR_MAX_WIDTH = 160;
    private static final int SELECTED_PACKS_MIN_WIDTH = 104;
    private static final int SELECTED_PACKS_MAX_WIDTH = 150;
    private static final int SELECTED_PACKS_MIN_SCREEN_WIDTH = 400;

    private static final int SIDEBAR_HEADER_HEIGHT = 32;
    private static final int PANEL_HEADER_HEIGHT = 24;
    private static final int TITLE_Y = 6;
    private static final int SUBTITLE_Y = 17;
    private static final int HEADING_Y = 8;
    private static final int LAYOUT_BUTTONS_GAP = 2;
    private static final int HEADER_BUTTONS_WIDTH = IconButtonWidget.BUTTON_SIZE * 3 + LAYOUT_BUTTONS_GAP + WIDGET_MARGIN;
    private static final int PANEL_DRAG_MIN_WIDTH = 80;
    /** A panel dragged narrower than this gets hidden. */
    private static final int PANEL_HIDE_WIDTH = 40;
    private static final int SHOW_BUTTON_MARGIN = 2;
    private static final int SHOW_BUTTON_WIDTH = 9;
    private static final int SHOW_BUTTON_HEIGHT = 25; // Odd, like the icon, to center it exactly
    private static final int HIDDEN_PANEL_INSET = SHOW_BUTTON_MARGIN * 2 + SHOW_BUTTON_WIDTH;
    private static final int PACKS_MIN_WIDTH = 160;
    private static final int DIVIDER_GRAB_MARGIN = 1;
    private static final int DIVIDER_LINE_INSET = 1;

    private static final int SHARE_BUTTON_WIDTH = 60;
    private static final int DONE_BUTTON_WIDTH = 60;
    private static final int DOWNLOAD_BUTTON_WIDTH = 100;
    private static final int PROGRESS_BAR_HEIGHT = 24;
    private static final int PROGRESS_BAR_WIDTH = 200;
    private static final int PROGRESS_BAR_COLOR = Theme.ACCENT;

    private static final float PROGRESS_BAR_MAX_TIME = 20.0F;
    private static final float DOWNLOAD_MESSAGE_MAX_TIME = 120.0F;
    private static final float SHARE_MESSAGE_TIME = 200.0F;
    private static final float ERROR_MESSAGE_TIME = 160.0F;
    private static final float DEBUG_MESSAGE_TIME = 200.0F;

    private final Screen parent;
    private final Component subtitle;
    private final List<Category> categories;

    private Category currentCategory;

    private ProgressBarScreenPopup progressBar;
    private MessageScreenPopup sharePopup;
    private MessageScreenPopup errorPopup;
    // DEBUG
    @Nullable
    private MessageScreenPopup debugPopup;

    private CategorySelectionWidget categorySelector;
    private PackSelectionListWidget packSelector;
    private SelectedPacksListWidget selectedPacksList;
    private PackNameTextFieldWidget packNameField;
    private FlatButtonWidget shareButton;
    private MutableMessageButtonWidget downloadButton;
    private FlatButtonWidget doneButton;
    private IconButtonWidget listLayoutButton;
    private IconButtonWidget gridLayoutButton;
    private IconButtonWidget searchButton;
    private ReloadButtonWidget reloadButton;
    private FlatTextFieldWidget searchField;
    private boolean searching = false;
    private String searchQuery = "";
    private IconButtonWidget backButton;
    private IconButtonWidget forwardButton;
    private final Deque<HistoryEntry> backHistory = new ArrayDeque<>();
    private final Deque<HistoryEntry> forwardHistory = new ArrayDeque<>();
    private IconButtonWidget showSidebarButton;
    private IconButtonWidget showSelectedPacksButton;
    private boolean draggingSidebar = false;
    private boolean draggingSelectedPacks = false;
    private int widthBeforeDrag;

    @Nullable
    private String packName;
    @Nullable
    private PackSelectionModel.Entry pack;
    @Nullable
    private String defaultPackName;
    private int sidebarWidth;
    private int selectedPacksWidth;
    private boolean sidebarHidden;
    private boolean selectedPacksHidden;
    private boolean changed = false;

    /** The sidebar's Installed row: the packs in the Vanilla Tweaks zips in the resource pack folder. */
    private final Category installedCategory = new Category(INSTALLED_TEXT.getString(), new ArrayList<>());
    private InstalledPacks.Result installed = InstalledPacks.Result.EMPTY;
    private boolean installedScanned = false;
    private float downloadProgress = -1.0F;
    private float downloadMessageTime;
    @Nullable
    private SharePackRequestData lastShareData;
    @Nullable
    private String lastShareCode;

    private final PackSelectionHelper selectionHelper = new PackSelectionHelper();

    public RefreshScreen(Screen parent, Component subtitle) {
        super(TITLE);
        this.parent = parent;
        this.subtitle = subtitle;

        this.categories = VTDMod.rpCategories.getCategories();
        this.currentCategory = !this.categories.isEmpty() ? this.categories.get(0) : null;

        this.selectionHelper.addCallback((pack, category, selected) -> {
            this.changed = true;
            this.updateButtons();
        });
    }

    public RefreshScreen(Screen parent, Component subtitle, PackSelectionModel.Entry pack) {
        this(parent, subtitle);

        this.packName = pack.getTitle().getString().replaceAll("\\.zip$", "");
        this.pack = pack;
        this.defaultPackName = this.packName;
    }

    private void reloadCategories() {
        VTDMod.loadRpCategories();
        this.updateCategories(VTDMod.rpCategories);
    }

    private void updateCategories(RpCategories data) {
        if (this.searching) {
            this.closeSearch();
        }

        // The categories in the history are about to stop existing
        this.backHistory.clear();
        this.forwardHistory.clear();
        this.updateHistoryButtons();

        this.categories.clear();
        this.categories.addAll(data.getCategories());

        this.packSelector.updateCategories(this.categories);
        this.categorySelector.updateCategories(this.categories);

        this.selectionHelper.cleanUpSelection();
        this.selectedPacksList.update();

        this.currentCategory = !this.categories.isEmpty() ? this.categories.get(0) : null;
        this.categorySelector.setSelectedCategory(this.currentCategory);
        this.packSelector.setCategory(this.currentCategory);
    }

    @Nullable
    private String getPackName() {
        return this.packNameField != null ? this.packNameField.getValue() : this.packName;
    }

    private void download() {
        this.changed = false;
        if (DOWNLOAD_DISABLED || VTDMod.USE_LOCAL_CATEGORIES) return;

        this.downloadProgress = 0.0F;
        this.progressBar.show(PROGRESS_BAR_MAX_TIME, () -> this.downloadProgress, () -> this.downloadProgress = -1.0F);

        // Disable download and done button to keep the download running within the screen
        this.downloadButton.active = false;
        this.doneButton.active = false;
        this.packNameField.setEditable(false);
        this.packSelector.setEditable(false);

        DownloadPackRequestData data = DownloadPackRequestData.create(this.selectionHelper.getSelectedPacks());

        // noinspection ConstantConditions
        CompletableFuture<Boolean> download = VTDMod.executePackDownload(data, f -> this.downloadProgress = f,
                this.minecraft.getResourcePackDirectory(),
                this.packNameField.isBlank() ? null : this.packNameField.getValue());

        download.whenCompleteAsync((success, throwable) -> {
            this.updateButtons();
            this.doneButton.active = true;
            this.packNameField.setEditable(true);
            this.packSelector.setEditable(true);

            if (throwable != null) {
                this.progressBar.abortWait();
                VTDMod.LOGGER.error("Pack download failed", throwable);
                this.errorPopup.show(ERROR_MESSAGE_TIME, DOWNLOAD_FAILED_TEXT.copy()
                        .append("\n").append(throwable.getLocalizedMessage()));
                this.downloadButton.setMessage(DOWNLOAD_FAILED_TEXT);
                return;
            }

            if (success) {
                this.downloadProgress = 1.0F;
                VTDMod.LOGGER.info("Pack downloaded successfully");
                this.scanInstalled();
                this.downloadButton.setMessage(DOWNLOAD_SUCCESS_TEXT);
            } else {
                this.progressBar.abortWait();
                VTDMod.LOGGER.error("Pack download failed");
                this.downloadButton.setMessage(DOWNLOAD_FAILED_TEXT);
            }
        }).completeOnTimeout(false, Constants.PACK_DOWNLOAD_TIMEOUT, TimeUnit.SECONDS);
    }

    private void share() {
        if (!this.selectionHelper.hasSelection()) {
            return;
        }

        SharePackRequestData data = new SharePackRequestData("resourcepacks", VTDMod.VT_VERSION,
                this.selectionHelper.getSelectedPacksPrimitive());
        if (data.equals(this.lastShareData) && this.lastShareCode != null) {
            this.showSharePopup(this.lastShareCode);
            return;
        }

        if (DEBUG_SHARE) {
            this.showSharePopup(DEBUG_SHARE_CODE);
            return;
        }

        VTDMod.executeShare(data).whenComplete((code, throwable) -> {
            // Execute on render thread since showSharePopup needs to access render system
            this.minecraft.execute(() -> {
                if (throwable != null) {
                    VTDMod.LOGGER.error("Failed to get resource pack share code", throwable);
                    this.errorPopup.show(ERROR_MESSAGE_TIME, SHARE_FAILED_TEXT.copy()
                            .append("\n").append(throwable.getLocalizedMessage()));
                    return;
                }

                this.lastShareData = data;
                this.lastShareCode = code;

                this.showSharePopup(code);
            });
        });
    }

    private void showSharePopup(String code) {
        if (code != null && this.sharePopup != null) {
            String url = VTDMod.BASE_URL + "/share#" + code;
            Component message = SHARE_CODE_TEXT.apply(Util.urlText(url));
            this.sharePopup.show(SHARE_MESSAGE_TIME, message);
        }
    }

    private void readResourcePack() {
        // #AbstractPack and its inheritors are private
        if (this.pack != null && this.pack.getClass().isNestmateOf(PackSelectionModel.Entry.class)) {
            net.minecraft.server.packs.repository.Pack profile = ((EntryBaseAccess) this.pack).vtdownloader$getPack();

            VTDMod.readResourcePackData(profile).whenCompleteAsync((selection, throwable) -> {
                if (throwable != null) {
                    if (this.errorPopup != null) {
                        this.errorPopup.show(ERROR_MESSAGE_TIME, READ_PACK_DATA_FAILED_TEXT.copy()
                                .append("\n").append(throwable.getLocalizedMessage()));
                    }
                    VTDMod.LOGGER.error("Failed to read VanillaTweaks pack data", throwable);
                } else {
                    this.selectionHelper.setSelection(selection);
                    this.selectedPacksList.update();
                    this.packSelector.updateSelection();
                    this.updateButtons();
                }
            });
        }

        this.pack = null;
    }

    private void scanInstalled() {
        InstalledPacks.scan(this.minecraft.getResourcePackDirectory()).thenAcceptAsync(result -> {
            this.installed = result;
            if (this.packSelector != null) {
                this.applyInstalled();
            }
        }, this.minecraft);
    }

    private void applyInstalled() {
        this.packSelector.setInstalled(this.installedCategory, this.installed.bySource());
    }

    /**
     * The arrow keys move a cursor over the packs, and Space or Enter selects the one it's on, with Shift for a range.
     * In the search field, Up, Down and Enter still work, so results can be picked without leaving it;
     * the pack name field keeps all of its keys.
     */
    private boolean handlePackKeys(KeyEvent event) {
        if (event.hasControlDownWithQuirk() || event.hasAltDown()) {
            return false;
        }

        boolean inSearch = this.getFocused() == this.searchField;
        boolean inText = this.getFocused() instanceof EditBox;
        if (inText && !inSearch) {
            return false;
        }

        int key = event.key();
        if (key == InputConstants.KEY_UP || key == InputConstants.KEY_DOWN) {
            return this.packSelector.moveCursor(0, key == InputConstants.KEY_UP ? -1 : 1);
        }
        if (!inText && (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_RIGHT)) {
            return this.packSelector.moveCursor(key == InputConstants.KEY_LEFT ? -1 : 1, 0);
        }
        if (key == InputConstants.KEY_RETURN || key == InputConstants.KEY_NUMPADENTER || (!inText && key == InputConstants.KEY_SPACE)) {
            return this.packSelector.activateCursor(event.hasShiftDown());
        }

        return false;
    }

    /** Ctrl+Tab and Ctrl+Shift+Tab: the next or previous row of the sidebar, wrapping around. */
    private void selectAdjacentCategory(int offset) {
        List<Category> rows = this.categorySelector.getRowCategories();
        if (rows.isEmpty()) {
            return;
        }

        Category current = this.currentCategory instanceof Category.SubCategory sub ? sub.getParent() : this.currentCategory;
        int index = this.searching ? -1 : rows.indexOf(current);
        int next = index == -1 ? (offset > 0 ? 0 : rows.size() - 1) : Math.floorMod(index + offset, rows.size());
        this.selectCategory(rows.get(next));
    }

    public boolean selectCategory(Category category) {
        // Even the category already shown: its row in the sidebar is the way back from a pack's page
        boolean closedDetails = this.packSelector.isDetailsOpen();
        this.packSelector.closeDetails();
        boolean wasSearching = this.searching;
        if (wasSearching) {
            this.closeSearch();
        }

        if (this.currentCategory != category) {
            this.backHistory.push(this.currentHistoryEntry());
            this.forwardHistory.clear();
            this.showCategory(category);
            this.updateHistoryButtons();
            return true;
        }

        return wasSearching || closedDetails;
    }

    private void showCategory(Category category) {
        this.currentCategory = category;
        this.categorySelector.setSelectedCategory(category);
        this.packSelector.setCategory(category);
    }

    private HistoryEntry currentHistoryEntry() {
        return new HistoryEntry(this.currentCategory, this.packSelector.scrollAmount());
    }

    /**
     * Back and forward work like a browser's, returning to where the list was scrolled. A pack's page is closed
     * first, then search.
     */
    private void goBack() {
        if (this.packSelector.isDetailsOpen()) {
            this.packSelector.closeDetails();
        } else if (this.searching) {
            this.closeSearch();
        } else {
            this.moveInHistory(this.backHistory, this.forwardHistory);
        }
    }

    private void goForward() {
        if (this.searching) {
            this.closeSearch();
        }

        this.moveInHistory(this.forwardHistory, this.backHistory);
    }

    private void moveInHistory(Deque<HistoryEntry> from, Deque<HistoryEntry> to) {
        if (!from.isEmpty()) {
            HistoryEntry entry = from.pop();
            to.push(this.currentHistoryEntry());
            this.showCategory(entry.category());
            this.packSelector.setScrollAmount(entry.scroll());
        }

        this.updateHistoryButtons();
    }

    private void updateHistoryButtons() {
        if (this.backButton != null) {
            this.backButton.active = this.searching || this.packSelector.isDetailsOpen() || !this.backHistory.isEmpty();
            this.forwardButton.active = !this.forwardHistory.isEmpty();
        }
    }

    /** Called by the list when a pack's page opens or closes. */
    public void onDetailsChanged() {
        this.updateHistoryButtons();
    }

    private int getHistoryButtonsRight() {
        return this.getCenterLeft() + WIDGET_MARGIN + IconButtonWidget.BUTTON_SIZE * 2 + LAYOUT_BUTTONS_GAP;
    }

    public void goToPack(Pack pack, Category category) {
        this.selectCategory(category);

        this.packSelector.focusPack(pack);
    }

    @SuppressWarnings("ConstantConditions") // client is marked as nullable
    @Override
    public void onClose() {
        if (this.changed && this.selectionHelper.hasSelection()) {
            this.minecraft.gui.setScreen(new UnsavedPackWarningScreen(this, this.parent));
        } else {
            this.minecraft.gui.setScreen(this.parent);
        }
    }

    private int getPanelsBottom() {
        return this.height - BOTTOM_BAR_HEIGHT;
    }

    private int getCenterLeft() {
        return this.sidebarHidden ? HIDDEN_PANEL_INSET : GAP * 2 + this.sidebarWidth;
    }

    private int getCenterRight() {
        if (!this.selectedPacksFit()) {
            return this.width - GAP;
        }

        return this.width - (this.selectedPacksHidden ? HIDDEN_PANEL_INSET : GAP * 2 + this.selectedPacksWidth);
    }

    /** Whether the window is wide enough for the selected packs panel. */
    private boolean selectedPacksFit() {
        return this.width >= SELECTED_PACKS_MIN_SCREEN_WIDTH;
    }

    private boolean isSelectedPacksShown() {
        return this.selectedPacksFit() && !this.selectedPacksHidden;
    }

    @Override
    protected void init() {
        this.sidebarHidden = ScreenSettings.isSidebarHidden();
        this.selectedPacksHidden = ScreenSettings.isSelectedPacksHidden();
        int savedSelectedPacksWidth = ScreenSettings.getSelectedPacksWidth();
        this.selectedPacksWidth = savedSelectedPacksWidth >= 0 ? savedSelectedPacksWidth
                : Math.clamp(this.width / 5, SELECTED_PACKS_MIN_WIDTH, SELECTED_PACKS_MAX_WIDTH);
        int savedSidebarWidth = ScreenSettings.getSidebarWidth();
        this.sidebarWidth = this.clampSidebarWidth(savedSidebarWidth >= 0 ? savedSidebarWidth
                : Math.clamp(this.width / 4, SIDEBAR_MIN_WIDTH, SIDEBAR_MAX_WIDTH));
        this.selectedPacksWidth = this.clampSelectedPacksWidth(this.selectedPacksWidth);

        int panelsBottom = this.getPanelsBottom();
        int listsTop = GAP + PANEL_HEADER_HEIGHT;
        int listsHeight = panelsBottom - listsTop;

        // Draw before everything else
        this.packSelector = this.addRenderableOnly(new PackSelectionListWidget(this.minecraft, this,
                this.getCenterLeft(), this.getCenterRight() - this.getCenterLeft(), listsHeight, listsTop,
                this.currentCategory, this.selectionHelper));
        this.packSelector.updateCategories(this.categories);
        this.applyInstalled();
        if (!this.installedScanned) {
            this.installedScanned = true;
            this.scanInstalled();
        }

        // Hidden when there's no room for it, but still kept up to date in case there is after a resize
        this.selectedPacksList = this.addRenderableOnly(new SelectedPacksListWidget(this, this.minecraft,
                this.selectedPacksWidth, listsHeight,
                this.width - GAP - this.selectedPacksWidth, listsTop,
                this.selectionHelper));

        // Reload button
        int reloadX = GAP + this.sidebarWidth - ReloadButtonWidget.BUTTON_SIZE - WIDGET_MARGIN;
        this.reloadButton = this.addRenderableWidget(new ReloadButtonWidget(reloadX, GAP + WIDGET_MARGIN,
                Constants.RESOURCE_PACK_RELOAD_TEXT, button -> this.reloadCategories()));

        if (DEBUG_BUTTON) this.addRenderableWidget(new DebugButtonWidget(reloadX - WIDGET_MARGIN - ReloadButtonWidget.BUTTON_SIZE,
                GAP + WIDGET_MARGIN, PLACEHOLDER_TEXT, button -> {
            if (this.debugPopup != null) this.debugPopup.show(DEBUG_MESSAGE_TIME, PLACEHOLDER_TEXT);
        }));

        // History
        this.backButton = this.addRenderableWidget(new IconButtonWidget(this.getCenterLeft() + WIDGET_MARGIN, GAP + WIDGET_MARGIN,
                BACK_ICON, BACK_TEXT, button -> this.goBack()));
        this.forwardButton = this.addRenderableWidget(new IconButtonWidget(0, GAP + WIDGET_MARGIN,
                FORWARD_ICON, FORWARD_TEXT, button -> this.goForward()));

        // Pack layout
        int gridButtonX = this.getCenterRight() - WIDGET_MARGIN - IconButtonWidget.BUTTON_SIZE;
        int listButtonX = gridButtonX - LAYOUT_BUTTONS_GAP - IconButtonWidget.BUTTON_SIZE;
        this.listLayoutButton = this.addRenderableWidget(new IconButtonWidget(listButtonX, GAP + WIDGET_MARGIN, LIST_ICON,
                LIST_LAYOUT_TEXT, button -> this.setPackLayout(PackLayout.LIST)));
        this.gridLayoutButton = this.addRenderableWidget(new IconButtonWidget(gridButtonX, GAP + WIDGET_MARGIN, GRID_ICON,
                GRID_LAYOUT_TEXT, button -> this.setPackLayout(PackLayout.GRID)));
        this.updateLayoutButtons();

        int searchButtonX = listButtonX - WIDGET_MARGIN - IconButtonWidget.BUTTON_SIZE;
        this.searchButton = this.addRenderableWidget(new IconButtonWidget(searchButtonX, GAP + WIDGET_MARGIN, SEARCH_ICON,
                SEARCH_BUTTON_TEXT, button -> {
            if (this.searching) {
                this.closeSearch();
            } else {
                this.openSearch();
            }
        }));

        int categoriesTop = GAP + SIDEBAR_HEADER_HEIGHT;
        this.categorySelector = this.addRenderableWidget(new CategorySelectionWidget(this, GAP, categoriesTop,
                this.sidebarWidth, panelsBottom - categoriesTop));
        this.categorySelector.setInstalledCategory(this.installedCategory, INSTALLED_TEXT);
        this.categorySelector.init(this.categories, this.currentCategory);

        this.searchField = this.addRenderableWidget(new FlatTextFieldWidget(this.font, this.getSearchFieldLeft(),
                GAP + WIDGET_MARGIN, this.getSearchFieldWidth(), IconButtonWidget.BUTTON_SIZE, SEARCH_PLACEHOLDER_TEXT, true));
        this.searchField.setMaxLength(PackNameTextFieldWidget.MAX_LENGTH);
        this.searchField.setResponder(this::onSearchChanged);
        this.searchField.visible = this.searching;
        if (this.searching) {
            // Laid out again with the search open, as after a resize
            this.searchField.setValue(this.searchQuery);
            this.setFocused(this.searchField);
            this.searchButton.setStyle(FlatButtonWidget.Style.PRIMARY);
        }

        // Bring back a hidden panel
        int showButtonY = GAP + (panelsBottom - GAP - SHOW_BUTTON_HEIGHT) / 2;
        this.showSidebarButton = this.addRenderableWidget(new IconButtonWidget(SHOW_BUTTON_MARGIN, showButtonY,
                SHOW_BUTTON_WIDTH, SHOW_BUTTON_HEIGHT, SHOW_SIDEBAR_ICON, SHOW_SIDEBAR_TEXT, button -> this.showSidebar()));
        this.showSelectedPacksButton = this.addRenderableWidget(new IconButtonWidget(
                this.width - SHOW_BUTTON_MARGIN - SHOW_BUTTON_WIDTH, showButtonY, SHOW_BUTTON_WIDTH, SHOW_BUTTON_HEIGHT,
                SHOW_SELECTED_PACKS_ICON, SHOW_SELECTED_PACKS_TEXT, button -> this.showSelectedPacks()));

        // Handle clicks before the pack selector
        this.sharePopup = this.addWidget(new MessageScreenPopup(this.minecraft, this,
                this.width / 2, this.height / 2,
                this.width / 2, (int) (this.height / 1.5), SHARE_TEXT));
        this.errorPopup = this.addWidget(new MessageScreenPopup(this.minecraft, this,
                this.width / 2, this.height / 2,
                this.width / 2, (int) (this.height / 1.5), Constants.ERROR_TEXT));
        if (DEBUG_BUTTON) this.debugPopup = this.addWidget(new MessageScreenPopup(this.minecraft, this,
                this.width / 2, this.height / 2,
                this.width / 2, (int) (this.height / 1.5), PLACEHOLDER_TEXT));

        this.addWidget(this.packSelector);
        this.addWidget(this.selectedPacksList);

        // The bottom bar: the actions on the right, and the name of what they make taking the rest of the room
        int barY = panelsBottom + (BOTTOM_BAR_HEIGHT - WIDGET_HEIGHT) / 2;
        int doneX = this.width - GAP - DONE_BUTTON_WIDTH;
        int downloadX = doneX - WIDGET_MARGIN - DOWNLOAD_BUTTON_WIDTH;
        int shareX = downloadX - WIDGET_MARGIN - SHARE_BUTTON_WIDTH;
        int nameFieldWidth = shareX - WIDGET_MARGIN - GAP;

        // noinspection ConstantConditions
        this.packNameField = this.addRenderableWidget(new PackNameTextFieldWidget(this.font,
                GAP, barY, nameFieldWidth, WIDGET_HEIGHT, this.getPackName(), PACK_NAME_FIELD_TEXT,
                this.minecraft.getResourcePackDirectory(), this.defaultPackName));
        this.packNameField.setResponder(s -> this.updateButtons());
        this.packName = null; // Pack name should only be used once

        this.shareButton = this.addRenderableWidget(new FlatButtonWidget(shareX, barY, SHARE_BUTTON_WIDTH, WIDGET_HEIGHT,
                SHARE_TEXT, button -> this.share(), FlatButtonWidget.Style.SECONDARY));

        this.downloadButton = this.addRenderableWidget(new MutableMessageButtonWidget(downloadX, barY,
                DOWNLOAD_BUTTON_WIDTH, WIDGET_HEIGHT, DOWNLOAD_TEXT, button -> this.download(), FlatButtonWidget.Style.PRIMARY));

        this.doneButton = this.addRenderableWidget(new FlatButtonWidget(doneX, barY, DONE_BUTTON_WIDTH, WIDGET_HEIGHT,
                CommonComponents.GUI_DONE, button -> this.onClose(), FlatButtonWidget.Style.SECONDARY));

        // Render over everything else
        this.progressBar = this.addRenderableOnly(new ProgressBarScreenPopup(this.minecraft, this.width / 2, this.height / 2,
                PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT, PROGRESS_BAR_COLOR));
        this.addRenderableOnly(this.sharePopup);
        this.addRenderableOnly(this.errorPopup);
        if (this.debugPopup != null) this.addRenderableOnly(this.debugPopup);

        this.layoutPanels();
        this.updateHistoryButtons();
        this.updateButtons();
        this.readResourcePack();
    }

    private int getSearchFieldLeft() {
        return this.getHistoryButtonsRight() + WIDGET_MARGIN;
    }

    private int getSearchFieldWidth() {
        return this.getCenterRight() - WIDGET_MARGIN * 2 - HEADER_BUTTONS_WIDTH - this.getSearchFieldLeft();
    }

    private void openSearch() {
        this.searching = true;
        this.updateHistoryButtons();
        this.searchField.visible = true;
        this.searchButton.setStyle(FlatButtonWidget.Style.PRIMARY);
        this.setFocused(this.searchField);
        this.searchField.moveCursorToEnd(false);
        this.searchField.setHighlightPos(0);
    }

    private void closeSearch() {
        this.searching = false;
        this.updateHistoryButtons();
        this.searchField.visible = false;
        this.searchButton.setStyle(FlatButtonWidget.Style.SECONDARY);
        if (this.getFocused() == this.searchField) {
            this.setFocused(null);
        }

        this.searchField.setValue("");
        this.packSelector.setSearch("");
        this.categorySelector.setSelectedCategory(this.currentCategory);
    }

    private void onSearchChanged(String query) {
        this.searchQuery = query;
        if (this.searching) {
            this.packSelector.setSearch(query);
            // No category is the current one while showing results, so that any can be clicked to leave them
            this.categorySelector.setSelectedCategory(query.isBlank() ? this.currentCategory : null);
        }
    }

    private static int clampPanelWidth(int width, int room) {
        return Math.clamp(width, PANEL_DRAG_MIN_WIDTH, Math.max(PANEL_DRAG_MIN_WIDTH, room));
    }

    private int clampSidebarWidth(int width) {
        return clampPanelWidth(width, this.getCenterRight() - PACKS_MIN_WIDTH - GAP * 2);
    }

    private int clampSelectedPacksWidth(int width) {
        return clampPanelWidth(width, this.width - this.getCenterLeft() - PACKS_MIN_WIDTH - GAP * 2);
    }

    /** Moves everything to fit the current size of the side panels, without losing the state of the lists. */
    private void layoutPanels() {
        this.categorySelector.setWidth(this.sidebarWidth);
        this.categorySelector.setVisible(!this.sidebarHidden);
        this.reloadButton.setX(GAP + this.sidebarWidth - ReloadButtonWidget.BUTTON_SIZE - WIDGET_MARGIN);
        this.reloadButton.visible = !this.sidebarHidden;
        this.showSidebarButton.visible = this.sidebarHidden;

        this.selectedPacksList.updateSizeAndPosition(this.selectedPacksWidth, this.selectedPacksList.getHeight(),
                this.width - GAP - this.selectedPacksWidth, this.selectedPacksList.getY());
        this.selectedPacksList.visible = this.isSelectedPacksShown();
        this.showSelectedPacksButton.visible = this.selectedPacksFit() && this.selectedPacksHidden;

        this.backButton.setX(this.getCenterLeft() + WIDGET_MARGIN);
        this.forwardButton.setX(this.getHistoryButtonsRight() - IconButtonWidget.BUTTON_SIZE);

        int gridButtonX = this.getCenterRight() - WIDGET_MARGIN - IconButtonWidget.BUTTON_SIZE;
        int listButtonX = gridButtonX - LAYOUT_BUTTONS_GAP - IconButtonWidget.BUTTON_SIZE;
        this.gridLayoutButton.setX(gridButtonX);
        this.listLayoutButton.setX(listButtonX);
        this.searchButton.setX(listButtonX - WIDGET_MARGIN - IconButtonWidget.BUTTON_SIZE);
        this.searchField.setBoxBounds(this.getSearchFieldLeft(), this.getSearchFieldWidth());

        int centerWidth = this.getCenterRight() - this.getCenterLeft();
        if (centerWidth != this.packSelector.getWidth() || this.getCenterLeft() != this.packSelector.getX()) {
            this.packSelector.updateSizeAndPosition(centerWidth, this.packSelector.getHeight(),
                    this.getCenterLeft(), this.packSelector.getY());
            this.packSelector.onWidthChanged();
        }
    }

    private void dragSidebar(int width) {
        this.sidebarHidden = width < PANEL_HIDE_WIDTH;
        if (!this.sidebarHidden) {
            this.sidebarWidth = this.clampSidebarWidth(width);
        }

        this.layoutPanels();
    }

    private void dragSelectedPacks(int width) {
        this.selectedPacksHidden = width < PANEL_HIDE_WIDTH;
        if (!this.selectedPacksHidden) {
            this.selectedPacksWidth = this.clampSelectedPacksWidth(width);
        }

        this.layoutPanels();
    }

    private void showSidebar() {
        this.sidebarHidden = false;
        // Only squeezed for this window, the width that was chosen is kept for a bigger one
        ScreenSettings.setSidebar(this.sidebarWidth, false);
        this.sidebarWidth = this.clampSidebarWidth(this.sidebarWidth);
        this.layoutPanels();
    }

    private void showSelectedPacks() {
        this.selectedPacksHidden = false;
        ScreenSettings.setSelectedPacks(this.selectedPacksWidth, false);
        this.selectedPacksWidth = this.clampSelectedPacksWidth(this.selectedPacksWidth);
        this.layoutPanels();
    }

    private int getSidebarDividerLeft() {
        return GAP + this.sidebarWidth;
    }

    private int getSelectedPacksDividerLeft() {
        return this.width - GAP * 2 - this.selectedPacksWidth;
    }

    private boolean isOverDivider(int left, double mouseX, double mouseY) {
        return mouseX >= left - DIVIDER_GRAB_MARGIN && mouseX < left + GAP + DIVIDER_GRAB_MARGIN
                && mouseY >= GAP && mouseY < this.getPanelsBottom();
    }

    private boolean isOverSidebarDivider(double mouseX, double mouseY) {
        return !this.sidebarHidden && this.isOverDivider(this.getSidebarDividerLeft(), mouseX, mouseY);
    }

    private boolean isOverSelectedPacksDivider(double mouseX, double mouseY) {
        return this.isSelectedPacksShown() && this.isOverDivider(this.getSelectedPacksDividerLeft(), mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        // The side buttons come 3 and 4 after the left button, whichever number that one has
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT + 3) {
            this.goBack();
            return true;
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT + 4) {
            this.goForward();
            return true;
        }

        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            if (this.isOverSidebarDivider(event.x(), event.y())) {
                this.draggingSidebar = true;
                this.widthBeforeDrag = this.sidebarWidth;
                return true;
            }

            if (this.isOverSelectedPacksDivider(event.x(), event.y())) {
                this.draggingSelectedPacks = true;
                this.widthBeforeDrag = this.selectedPacksWidth;
                return true;
            }
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (this.draggingSidebar) {
            this.dragSidebar((int) event.x() - GAP);
            return true;
        }

        if (this.draggingSelectedPacks) {
            this.dragSelectedPacks(this.width - GAP - (int) event.x());
            return true;
        }

        return super.mouseDragged(event, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (this.draggingSidebar) {
            this.draggingSidebar = false;
            // Bringing it back should give the width it had, not the narrowest it passed through on the way out
            if (this.sidebarHidden) {
                this.sidebarWidth = this.widthBeforeDrag;
            }

            ScreenSettings.setSidebar(this.sidebarWidth, this.sidebarHidden);
            return true;
        }

        if (this.draggingSelectedPacks) {
            this.draggingSelectedPacks = false;
            if (this.selectedPacksHidden) {
                this.selectedPacksWidth = this.widthBeforeDrag;
            }

            ScreenSettings.setSelectedPacks(this.selectedPacksWidth, this.selectedPacksHidden);
            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // The quirk is that it means Cmd on macOS
        if (event.hasControlDownWithQuirk() && event.key() == InputConstants.KEY_F) {
            this.openSearch();
            return true;
        }

        if (event.hasControlDownWithQuirk() && event.key() == InputConstants.KEY_LBRACKET) {
            this.goBack();
            return true;
        }

        if (event.hasControlDownWithQuirk() && event.key() == InputConstants.KEY_RBRACKET) {
            this.goForward();
            return true;
        }

        // Ctrl+Tab even on macOS, where Cmd+Tab belongs to the system
        if ((event.hasControlDown() || event.hasControlDownWithQuirk()) && event.key() == InputConstants.KEY_TAB) {
            this.selectAdjacentCategory(event.hasShiftDown() ? -1 : 1);
            return true;
        }

        if (event.hasControlDownWithQuirk() && event.key() >= InputConstants.KEY_1 && event.key() <= InputConstants.KEY_9) {
            int index = event.key() - InputConstants.KEY_1;
            if (index < this.categories.size()) {
                this.selectCategory(this.categories.get(index));
            }
            return true;
        }

        if (event.hasControlDownWithQuirk() && event.key() == InputConstants.KEY_A
                && !(this.getFocused() instanceof EditBox)) {
            if (event.hasShiftDown()) {
                this.clearSelection();
            } else {
                this.packSelector.selectAllShown();
            }
            return true;
        }

        if (this.handlePackKeys(event)) {
            return true;
        }

        if (this.packSelector.isDetailsOpen() && event.isEscape()) {
            this.packSelector.closeDetails();
            return true;
        }

        if (this.searching && event.isEscape()) {
            this.closeSearch();
            return true;
        }

        return super.keyPressed(event);
    }

    private void clearSelection() {
        if (!this.packSelector.isEditable() || !this.selectionHelper.hasSelection()) {
            return;
        }

        this.selectionHelper.clearSelection();
        this.packSelector.updateSelection();
        this.selectedPacksList.update();
        this.changed = true;
        this.updateButtons();
    }

    private void setPackLayout(PackLayout layout) {
        ScreenSettings.setPackLayout(layout);
        this.packSelector.setLayout(layout);
        this.updateLayoutButtons();
    }

    private void updateLayoutButtons() {
        boolean grid = ScreenSettings.getPackLayout() == PackLayout.GRID;
        this.listLayoutButton.setStyle(grid ? FlatButtonWidget.Style.SECONDARY : FlatButtonWidget.Style.PRIMARY);
        this.gridLayoutButton.setStyle(grid ? FlatButtonWidget.Style.PRIMARY : FlatButtonWidget.Style.SECONDARY);
    }

    private void updateButtons() {
        if (this.shareButton != null) {
            this.shareButton.active = this.selectionHelper.hasSelection();
        }
        if (this.downloadButton != null) {
            this.downloadButton.active = this.selectionHelper.hasSelection() && this.packNameField.canUseName();
        }
    }

    public boolean isCoveredByPopup(int mouseX, int mouseY) {
        return this.progressBar.isMouseOver(mouseX, mouseY)
                || this.sharePopup.isMouseOver(mouseX, mouseY)
                || this.errorPopup.isMouseOver(mouseX, mouseY)
                || (this.debugPopup != null && this.debugPopup.isMouseOver(mouseX, mouseY));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, Theme.BLACK);

        int bottom = this.getPanelsBottom();
        if (!this.sidebarHidden) {
            graphics.fill(GAP, GAP, GAP + this.sidebarWidth, bottom, Theme.PANEL);
        }

        graphics.fill(this.getCenterLeft(), GAP, this.getCenterRight(), bottom, Theme.PANEL);
        if (this.isSelectedPacksShown()) {
            graphics.fill(this.width - GAP - this.selectedPacksWidth, GAP, this.width - GAP, bottom, Theme.PANEL);
        }
    }

    /**
     * A frame can be drawn after the window shrinks but before this screen is laid out for the new size. What's now
     * outside the window can't be clipped to it, which crashes the game on some versions, so such a frame is skipped.
     */
    private boolean isLaidOutForWindow() {
        Window window = this.minecraft.getWindow();
        int scale = window.getGuiScale();
        return (this.width - 1) * scale < window.getWidth() && (this.height - 1) * scale < window.getHeight();
    }

    private void renderHeaders(GuiGraphicsExtractor graphics) {
        // Sidebar
        if (!this.sidebarHidden) {
            int titleWidth = this.sidebarWidth - PANEL_PADDING - ReloadButtonWidget.BUTTON_SIZE - WIDGET_MARGIN * 2;
            graphics.text(this.font, Theme.clip(this.font, this.title.getString(), titleWidth),
                    GAP + PANEL_PADDING, GAP + TITLE_Y, Theme.WHITE, false);
            graphics.text(this.font, Theme.clip(this.font, this.subtitle.getString(), titleWidth),
                    GAP + PANEL_PADDING, GAP + SUBTITLE_Y, Theme.DIM, false);
        }

        // Packs
        if (this.currentCategory != null && !this.searching) {
            int headingLeft = this.getHistoryButtonsRight() + WIDGET_MARGIN;
            int headingWidth = this.getCenterRight() - headingLeft - HEADER_BUTTONS_WIDTH - WIDGET_MARGIN * 2;
            graphics.text(this.font, Theme.clip(this.font, this.getCategoryHeading(), headingWidth),
                    headingLeft, GAP + HEADING_Y, Theme.WHITE, false);
        }

        // Selected packs
        if (this.isSelectedPacksShown()) {
            int left = this.width - GAP - this.selectedPacksWidth + PANEL_PADDING;
            int right = this.width - GAP - PANEL_PADDING;

            String count = String.valueOf(this.selectionHelper.getSelectionSize());
            int countX = right - this.font.width(count);
            graphics.text(this.font, count, countX, GAP + HEADING_Y,
                    this.selectionHelper.hasSelection() ? Theme.ACCENT : Theme.FAINT, false);
            graphics.text(this.font, Theme.clip(this.font, SELECTED_PACKS_TEXT.getString(), countX - left - WIDGET_MARGIN),
                    left, GAP + HEADING_Y, Theme.WHITE, false);

            if (!this.selectionHelper.hasSelection()) {
                graphics.text(this.font, Theme.clip(this.font, NOTHING_SELECTED_TEXT.getString(), right - left),
                        left, GAP + PANEL_HEADER_HEIGHT, Theme.FAINT, false);
            }
        }
    }

    private void renderDividers(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.draggingSidebar || this.isOverSidebarDivider(mouseX, mouseY)) {
            this.renderDivider(graphics, this.getSidebarDividerLeft(), !this.sidebarHidden, this.draggingSidebar);
        } else if (this.draggingSelectedPacks || this.isOverSelectedPacksDivider(mouseX, mouseY)) {
            this.renderDivider(graphics, this.getSelectedPacksDividerLeft(), !this.selectedPacksHidden, this.draggingSelectedPacks);
        }
    }

    private void renderDivider(GuiGraphicsExtractor graphics, int left, boolean panelShown, boolean dragging) {
        if (panelShown) {
            graphics.fill(left + DIVIDER_LINE_INSET, GAP, left + GAP - DIVIDER_LINE_INSET, this.getPanelsBottom(),
                    dragging ? Theme.ACCENT : Theme.TRACK_BG);
        }

        graphics.requestCursor(CursorTypes.RESIZE_EW);
    }

    private record HistoryEntry(Category category, double scroll) {
    }

    private String getCategoryHeading() {
        if (this.currentCategory instanceof Category.SubCategory subCategory) {
            return subCategory.getParent().getName() + " / " + subCategory.getName();
        }

        return this.currentCategory.getName();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.isLaidOutForWindow()) {
            return;
        }

        this.renderHeaders(graphics);
        this.renderDividers(graphics, mouseX, mouseY);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        this.renderDebugInfo(graphics, mouseX, mouseY);
        this.packSelector.renderTooltips(graphics, mouseX, mouseY);
        this.renderPackNameFieldTooltip(graphics, mouseX, mouseY);

        this.updateTime(delta);
    }

    private void renderDebugInfo(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.packSelector.renderDebugInfo(graphics, mouseX, mouseY);
        this.categorySelector.renderDebugInfo(graphics);

        if (!SHOW_DEBUG_INFO) return;
        Font textRenderer = this.minecraft.font;
        List<String> debugInfo = List.of(
                "Ch = " + this.children().stream().map(e -> e.getClass().getSimpleName())
                        .collect(Collectors.joining(", ")),
                "H = " + this.getChildAt(mouseX, mouseY),
                "Sm = " + this.sharePopup.isMouseOver(mouseX, mouseY),
                "MX/MY = " + mouseX + "/" + mouseY
        );

        RenderUtil.renderDebugInfo(graphics, textRenderer, 0, this.height, debugInfo);
    }

    private void renderPackNameFieldTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.packNameField.isMouseOver(mouseX, mouseY)) {
            Component text = this.packNameField.getTooltipText();
            if (text != null) {
                graphics.setTooltipForNextFrame(this.font, text, mouseX, mouseY);
            }
        }
    }

    private void updateTime(float delta) {
        if (this.downloadMessageTime >= DOWNLOAD_MESSAGE_MAX_TIME) {
            this.downloadMessageTime = 0.0F;
            this.downloadButton.resetMessage();
        } else {
            this.downloadMessageTime += delta;
        }
    }

    // for some reason the share popup is never reported as the hovered element
    @Override
    public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
        if (this.sharePopup.isMouseOver(mouseX, mouseY)) {
            return Optional.of(this.sharePopup);
        } else if (this.errorPopup.isMouseOver(mouseX, mouseY)) {
            return Optional.of(this.errorPopup);
        } else if (this.debugPopup != null && this.debugPopup.isMouseOver(mouseX, mouseY)) {
            return Optional.of(this.debugPopup);
        }

        return super.getChildAt(mouseX, mouseY);
    }
}
