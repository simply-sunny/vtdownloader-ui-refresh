package mix.vtdrefresh.gui.widget;

import me.bymartrixx.vtd.data.Category;
import me.bymartrixx.vtd.data.Pack;
import mix.vtdrefresh.gui.Theme;
import mix.vtdrefresh.gui.RefreshScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SelectedPacksListWidget extends AbstractSelectionList<SelectedPacksListWidget.AbstractEntry> {
    private static final Component HEADER = Component.translatable("vtd.selectedPacks");

    private static final int ITEM_HEIGHT = 14;
    private static final int ROW_LEFT_RIGHT_MARGIN = 2;
    private static final int CATEGORY_INDENT = 6;
    private static final int SUB_CATEGORY_INDENT = 10;
    private static final int PACK_INDENT = 12;
    private static final int SUB_CATEGORY_PACK_INDENT = 16;
    private static final int ACCENT_GAP = 4;
    private static final int ACCENT_WIDTH = 2;
    private static final long DOUBLE_CLICK_THRESHOLD = 250L;

    private final RefreshScreen screen;
    private final PackSelectionHelper selectionHelper;

    public SelectedPacksListWidget(RefreshScreen screen, Minecraft client, int width, int height, int x, int y,
                                   PackSelectionHelper selectionHelper) {
        super(client, width, height, y, ITEM_HEIGHT);
        this.screen = screen;
        this.selectionHelper = selectionHelper;

        this.setX(x);

        selectionHelper.addCallback((pack, category, selected) -> this.update());
        this.addPacks(selectionHelper.getSelectedPacks());
    }

    /**
     * Rebuilds the panel from the selection. Updating it a pack at a time left a parent category's heading behind
     * once its last subcategory pack was deselected.
     */
    public void update() {
        double scroll = this.scrollAmount();
        this.clearEntries();

        this.addPacks(this.selectionHelper.getSelectedPacks());
        this.setScrollAmount(scroll);
    }

    private void updateSelection(Pack pack, Category category, boolean selected) {
        if (selected) {
            CategoryEntry categoryEntry = this.getOrCreateCategoryEntry(category);
            PackEntry entry = this.getPackEntry(pack, category);
            int i = this.getLastChildEntryIndex(category);
            if (i == -1) {
                i = this.children().indexOf(categoryEntry);
            }

            // In 1.21.10+, children() returns unmodifiable list, use insertEntry helper
            this.insertEntryAt(i + 1, entry);
            return;
        }

        int i = this.getPackEntryIndex(pack);
        if (i == -1) {
            return;
        }

        // In 1.21.10+, use removeEntry instead of children().remove()
        this.removeEntry(this.children().get(i));

        int categoryIndex = this.getCategoryEntryIndex(category);
        if (categoryIndex != -1) {
            int lastChildIndex = this.getLastChildIndex(category);
            if (lastChildIndex == -1) {
                this.removeEntry(this.children().get(categoryIndex));
            }
        }

        // Update scrollbar
        this.refreshScrollAmount();
    }

    private void addPacks(Map<Category, List<Pack>> packs) {
        for (Category category : packs.keySet()) {
            packs.get(category).forEach(pack -> updateSelection(pack, category, true));
        }
    }

    private int getCategoryEntryIndex(Category category) {
        for (int i = 0; i < this.children().size(); i++) {
            //noinspection EqualsBetweenInconvertibleTypes - CategoryEntry overrides equals()
            if (this.children().get(i).equals(category)) {
                return i;
            }
        }

        return -1;
    }

    private CategoryEntry getOrCreateCategoryEntry(Category category) {
        int i = this.getCategoryEntryIndex(category);
        if (i == -1) {
            CategoryEntry entry;
            if (category instanceof Category.SubCategory subCategory) {
                entry = new SubCategoryEntry(this, subCategory);
                CategoryEntry parentEntry = this.getOrCreateCategoryEntry(subCategory.getParent());
                int index = this.getLastChildIndex(parentEntry.getCategory());
                if (index != -1) {
                    // In 1.21.10+, use insertEntryAt instead of children().add(index, entry)
                    this.insertEntryAt(index + 1, entry);
                } else {
                    // Add to end
                    this.addEntry(entry);
                }
            } else {
                entry = new CategoryEntry(this, category);
                // Add to end
                this.addEntry(entry);
            }
            return entry;
        }

        return (CategoryEntry) this.children().get(i);
    }

    // Since children() is unmodifiable in 1.21.10+ the only solution is to rebuild the list
    private void insertEntryAt(int index, AbstractEntry entry) {
        List<AbstractEntry> entries = new ArrayList<>(this.children());
        entries.add(index, entry);
        this.replaceEntries(entries);
    }

    private int getPackEntryIndex(Pack pack) {
        for (int i = 0; i < this.children().size(); i++) {
            //noinspection EqualsBetweenInconvertibleTypes - PackEntry overrides equals()
            if (this.children().get(i).equals(pack)) {
                return i;
            }
        }

        return -1;
    }

    private PackEntry getPackEntry(Pack pack, Category category) {
        int i = this.getPackEntryIndex(pack);
        if (i == -1) {
            return new PackEntry(this, category, pack);
        }

        return (PackEntry) this.children().get(i);
    }

    private int getLastChildEntryIndex(Category category) {
        int index = -1;
        int i = this.getCategoryEntryIndex(category);
        if (i != -1) {
            for (i++; i < this.children().size(); i++) {
                AbstractEntry entry = this.children().get(i);
                if (entry instanceof PackEntry packEntry) {
                    if (packEntry.category.equals(category)) {
                        index = i;
                    }
                    continue;
                }

                break;
            }
        }

        return index;
    }

    private int getLastSubCategoryIndex(Category category) {
        int index = -1;
        int i = this.getCategoryEntryIndex(category);
        if (i != -1 && !(category instanceof Category.SubCategory)) {
            for (i++; i < this.children().size(); i++) {
                AbstractEntry entry = this.children().get(i);
                if (entry instanceof SubCategoryEntry subCategoryEntry) {
                    if (subCategoryEntry.getParentCategory().equals(category)) {
                        index = i;
                    }
                    continue;
                }

                break;
            }
        }

        return index;
    }

    private int getLastChildIndex(Category category) {
        int subCat = this.getLastSubCategoryIndex(category);
        if (subCat != -1) {
            SubCategoryEntry entry = (SubCategoryEntry) this.children().get(subCat);
            int subCatIndex = this.getLastChildEntryIndex(entry.getParentCategory());
            if (subCatIndex != -1) {
                return subCatIndex;
            }
        }

        int index = this.getLastChildEntryIndex(category);
        if (index == -1 && subCat != -1) {
            return subCat;
        }

        return index;
    }

    @Override
    public int getRowWidth() {
        return this.width - ROW_LEFT_RIGHT_MARGIN * 2;
    }

    @Override
    public int getRowLeft() {
        return this.getX() + ROW_LEFT_RIGHT_MARGIN;
    }

    @Override
    protected int scrollBarX() {
        return this.getRight() - SCROLLBAR_WIDTH;
    }

    // private void moveFocus(MoveDirection direction) {
    //     int offset = direction == MoveDirection.UP ? -1 : 1;
    //     if (!this.children().isEmpty()) {
    //         AbstractEntry current = this.getFocused();
    //         int currentIndex = current != null ? this.children().indexOf(current) : -1;
    //
    //         int index = MathHelper.clamp(currentIndex + offset, 0, this.getEntryCount() - 1);
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

    // region input callbacks
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (this.isFocused()) {
            int keyCode = event.key();
            // if (keyCode == GLFW.GLFW_KEY_DOWN) {
            //     this.moveFocus(MoveDirection.DOWN);
            //     return true;
            // } else if (keyCode == GLFW.GLFW_KEY_UP) {
            //     this.moveFocus(MoveDirection.UP);
            //     return true;
            // }

            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                AbstractEntry focusedEntry = this.getFocused();
                if (focusedEntry != null) {
                    focusedEntry.selectEntry();
                }
            }
        }

        return false;
    }

    // @Override
    // public boolean changeFocus(boolean lookForwards) {
    //     if (this.extended) {
    //         return !this.isFocused();
    //     }
    //
    //     return false;
    // }
    // endregion

    // region render
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
        entry.extractContent(graphics, mouseX, mouseY, hovered, delta);
    }
    // endregion

    @Override
    public void updateWidgetNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, HEADER);
    }

    public static abstract class AbstractEntry extends Entry<AbstractEntry> {
        protected final Minecraft client;
        protected final SelectedPacksListWidget widget;
        private Component text;

        protected AbstractEntry(SelectedPacksListWidget widget) {
            this.client = widget.minecraft;
            this.widget = widget;
        }

        protected abstract String getTextString();

        protected abstract void selectEntry();

        protected Component getText() {
            if (this.text != null) {
                return this.text;
            }

            this.text = Component.nullToEmpty(this.getTextString());
            return this.text;
        }

        protected int getColor() {
            return Theme.DIM;
        }

        protected int getIndent() {
            return CATEGORY_INDENT;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            int x = this.getX();
            int y = this.getY();
            int width = this.getWidth();
            int height = this.getHeight();
            int indent = this.getIndent();

            if (hovered) {
                graphics.fill(x, y, x + width, y + height, Theme.ROW_HOVER);
            }

            int textY = y + (height - 8) / 2;
            Font font = this.client.font;
            String label = Theme.clip(font, this.getText().getString(), width - indent - SCROLLBAR_WIDTH);
            graphics.text(font, label, x + indent, textY, this.getColor(), false);
        }
    }

    public static class CategoryEntry extends AbstractEntry {
        protected final Category category;
        private long lastClickTime = -1;

        public CategoryEntry(SelectedPacksListWidget widget, Category category) {
            super(widget);
            this.category = category;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                long time = System.currentTimeMillis();
                if (time <= this.lastClickTime + DOUBLE_CLICK_THRESHOLD) {
                    this.selectEntry();
                }
                this.lastClickTime = time;
            }

            return super.mouseClicked(event, bl);
        }

        @Override
        protected String getTextString() {
            return this.category.getName();
        }

        @Override
        protected void selectEntry() {
            this.widget.screen.selectCategory(this.category);
        }

        public Category getCategory() {
            return this.category;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof CategoryEntry entry) {
                return this.category.equals(entry.category);
            } else if (obj instanceof Category c) {
                return this.category.equals(c);
            } else {
                return super.equals(obj);
            }
        }
    }

    public static class SubCategoryEntry extends CategoryEntry {
        public SubCategoryEntry(SelectedPacksListWidget widget, Category.SubCategory category) {
            super(widget, category);
        }

        @Override
        protected int getIndent() {
            return SUB_CATEGORY_INDENT;
        }

        @Override
        public Category.SubCategory getCategory() {
            return (Category.SubCategory) super.getCategory();
        }

        public Category getParentCategory() {
            return this.getCategory().getParent();
        }
    }

    public static class PackEntry extends AbstractEntry {
        private final Category category;
        private final Pack pack;

        private int color = 0;
        private int lastChildrenCount = -1;
        private long lastClickTime = -1;

        public PackEntry(SelectedPacksListWidget widget, Category category, Pack pack) {
            super(widget);
            this.category = category;
            this.pack = pack;
        }

        private int calculateColor() {
            int color = this.widget.selectionHelper.getSelectionColor(this.pack);

            return color != PackSelectionHelper.DEFAULT_SELECTION_COLOR ? color : Theme.ACCENT;
        }

        @Override
        protected int getIndent() {
            return this.category instanceof Category.SubCategory ? SUB_CATEGORY_PACK_INDENT : PACK_INDENT;
        }

        @Override
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            super.extractContent(graphics, mouseX, mouseY, hovered, tickDelta);

            int barX = this.getX() + this.getIndent() - ACCENT_GAP;
            graphics.fill(barX, this.getY() + 3, barX + ACCENT_WIDTH, this.getY() + this.getHeight() - 3, this.getAccentColor());
        }

        @Override
        protected int getColor() {
            return Theme.WHITE;
        }

        private int getAccentColor() {
            if (this.color != 0 && this.widget.children().size() == this.lastChildrenCount) {
                return this.color;
            }

            this.lastChildrenCount = this.widget.children().size();
            this.color = this.calculateColor();
            return this.color;
        }

        @Override
        protected String getTextString() {
            return this.pack.getName();
        }

        @Override
        protected void selectEntry() {
            this.widget.screen.goToPack(this.pack, this.category);
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
            if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
                long time = System.currentTimeMillis();
                if (time <= this.lastClickTime + DOUBLE_CLICK_THRESHOLD) {
                    this.selectEntry();
                }
                this.lastClickTime = time;
            }

            return super.mouseClicked(event, bl);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof PackEntry entry) {
                return this.pack.equals(entry.pack);
            } else if (obj instanceof Pack p) {
                return this.pack.equals(p);
            } else {
                return super.equals(obj);
            }
        }
    }
}
