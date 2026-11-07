package mix.vtdrefresh.gui.widget;

import me.bymartrixx.vtd.data.Category;
import mix.vtdrefresh.gui.Theme;
import mix.vtdrefresh.gui.RefreshScreen;
import me.bymartrixx.vtd.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.AbstractContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.platform.InputConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The sidebar: a vertical, scrolling list of the categories. */
public class CategorySelectionWidget extends AbstractContainerEventHandler implements Renderable, NarratableEntry {
    // DEBUG
    private static final boolean SHOW_DEBUG_INFO = false;

    private static final int ROW_HEIGHT = 16;
    private static final int ROW_MARGIN = 2;
    private static final int SCROLLBAR_AREA_WIDTH = 6;
    private static final int SCROLLBAR_MIN_HEIGHT = 16;
    private static final int SCROLL_ROWS = 2;
    /** Space between the scrolling list and the Installed row pinned under it. */
    private static final int PINNED_GAP = 4;

    private final List<CategoryButtonWidget> children = new ArrayList<>();
    private final Map<Category, CategoryButtonWidget> categoryButtons = new HashMap<>();
    private final RefreshScreen screen;
    private final int left;
    private final int top;
    private int right;
    private final int bottom;

    private List<Category> categories;
    @org.jetbrains.annotations.Nullable
    private Category selectedCategory;
    @org.jetbrains.annotations.Nullable
    private CategoryButtonWidget installedButton;
    private double scrollAmount;
    private boolean scrolling;
    private boolean visible = true;

    public CategorySelectionWidget(RefreshScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.left = x;
        this.top = y;
        this.right = x + width;
        this.bottom = y + height;
    }

    public void setWidth(int width) {
        this.right = this.left + width;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void init(List<Category> categories, Category selectedCategory) {
        this.setCategories(categories);
        this.initCategoryButtons();
        this.setSelectedCategory(selectedCategory);
    }

    public void updateCategories(List<Category> categories) {
        this.categories = new ArrayList<>(categories);
        this.categoryButtons.clear();
        this.initCategoryButtons();
        this.setScrollAmount(this.getScrollAmount());
    }

    /** Pins a row for {@code category} under the list, apart from the categories. */
    public void setInstalledCategory(Category category, Component name) {
        this.installedButton = new CategoryButtonWidget(this.screen, ROW_HEIGHT, name, category);
        this.installedButton.setSelected(category == this.selectedCategory);
    }

    /** The rows in order, as Ctrl+Tab moves through them: the categories, then Installed. */
    public List<Category> getRowCategories() {
        List<Category> rows = new ArrayList<>();
        for (CategoryButtonWidget button : this.children) {
            rows.add(button.getCategory());
        }
        if (this.installedButton != null) {
            rows.add(this.installedButton.getCategory());
        }
        return rows;
    }

    public void setCategories(List<Category> categories) {
        this.categories = categories;
    }

    /** One row per category. Subcategories are in the list, after their category's packs. */
    public void initCategoryButtons() {
        this.children.clear();
        for (Category category : this.categories) {
            this.children.add(this.getOrCreateCategoryButton(category));
        }
    }

    private CategoryButtonWidget getOrCreateCategoryButton(Category category) {
        return this.categoryButtons.computeIfAbsent(category, c ->
                new CategoryButtonWidget(this.screen, ROW_HEIGHT, Component.literal(c.getName()), c));
    }

    public void setSelectedCategory(Category category) {
        this.selectedCategory = category;
        this.initCategoryButtons();
        this.setScrollAmount(this.getScrollAmount());
        Category parent = category instanceof Category.SubCategory sub ? sub.getParent() : null;
        this.categoryButtons.forEach((c, button) -> {
            button.setSelected(c == category);
            button.setOpen(c == parent);
        });
        if (this.installedButton != null) {
            this.installedButton.setSelected(this.installedButton.getCategory() == category);
        }
        CategoryButtonWidget button = this.categoryButtons.get(category);
        if (button != null) {
            this.ensureVisible(button);
        }
    }

    /** The scrolling list's height, above the pinned Installed row. */
    private int getHeight() {
        return this.getListBottom() - this.top;
    }

    private int getListBottom() {
        return this.installedButton != null ? this.bottom - ROW_HEIGHT - PINNED_GAP : this.bottom;
    }

    private int getContentHeight() {
        return this.children.size() * ROW_HEIGHT;
    }

    private boolean shouldHaveScrollbar() {
        return this.getContentHeight() > this.getHeight();
    }

    private int getMaxScroll() {
        return Math.max(0, this.getContentHeight() - this.getHeight());
    }

    private double getScrollAmount() {
        return this.scrollAmount;
    }

    private void setScrollAmount(double scrollAmount) {
        this.scrollAmount = Mth.clamp(scrollAmount, 0.0, this.getMaxScroll());
    }

    private int getScrollbarHeight() {
        int height = this.getHeight() * this.getHeight() / Math.max(1, this.getContentHeight());
        return Mth.clamp(height, SCROLLBAR_MIN_HEIGHT, this.getHeight());
    }

    private boolean isOverScrollbar(double mouseX, double mouseY) {
        return this.shouldHaveScrollbar() && mouseX >= this.right - SCROLLBAR_AREA_WIDTH && mouseX < this.right
                && mouseY >= this.top && mouseY < this.getListBottom();
    }

    private void ensureVisible(CategoryButtonWidget button) {
        int rowTop = this.children.indexOf(button) * ROW_HEIGHT;
        if (rowTop < this.getScrollAmount()) {
            this.setScrollAmount(rowTop);
        } else if (rowTop + ROW_HEIGHT > this.getScrollAmount() + this.getHeight()) {
            this.setScrollAmount(rowTop + ROW_HEIGHT - this.getHeight());
        }
    }

    // region input callbacks
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (!this.visible) {
            return false;
        }

        this.scrolling = event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.isOverScrollbar(event.x(), event.y());
        if (this.scrolling) {
            return true;
        }

        return this.isMouseOver(event.x(), event.y()) && super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.scrolling = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double deltaX, double deltaY) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.scrolling) {
            int travel = Math.max(1, this.getHeight() - this.getScrollbarHeight());
            this.setScrollAmount(this.getScrollAmount() + deltaY * this.getMaxScroll() / travel);
            return true;
        }

        return false;
    }

    // Only called if isMouseOver is true; from Screen#mouseScrolled
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.setScrollAmount(this.getScrollAmount() - verticalAmount * ROW_HEIGHT * SCROLL_ROWS);
        return true;
    }

    // Needed to allow scrolling
    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible && mouseX >= this.left && mouseX < this.right && mouseY >= this.top && mouseY < this.bottom;
    }
    // endregion

    // region render
    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.visible) {
            return;
        }

        graphics.enableScissor(this.left, this.top, this.right, this.getListBottom());
        this.renderCategories(graphics, mouseX, mouseY);
        graphics.disableScissor();
        this.renderScrollbar(graphics, mouseX, mouseY);

        if (this.installedButton != null) {
            int y = this.bottom - ROW_HEIGHT;
            graphics.fill(this.left + ROW_MARGIN, y - PINNED_GAP / 2 - 1, this.right - ROW_MARGIN, y - PINNED_GAP / 2, Theme.ROW_HOVER);
            this.installedButton.render(graphics, this.left + ROW_MARGIN, y, this.right - this.left - ROW_MARGIN * 2, mouseX, mouseY);
        }
    }

    private void renderCategories(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = this.left + ROW_MARGIN;
        int width = this.right - this.left - ROW_MARGIN * 2;
        boolean mouseInList = this.isMouseOver(mouseX, mouseY) && mouseY < this.getListBottom()
                && !this.isOverScrollbar(mouseX, mouseY);

        for (int i = 0; i < this.children.size(); i++) {
            CategoryButtonWidget button = this.children.get(i);
            int y = this.top - (int) this.getScrollAmount() + i * ROW_HEIGHT;

            if (y + ROW_HEIGHT > this.top && y < this.getListBottom()) {
                // Off-list coordinates, so that a row under the cursor but outside the list isn't hovered
                button.render(graphics, x, y, width, mouseInList ? mouseX : -1, mouseInList ? mouseY : -1);
            } else {
                button.clearHover();
            }
        }
    }

    private void renderScrollbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (this.shouldHaveScrollbar()) {
            int size = this.getScrollbarHeight();
            int y = this.top + (int) (this.getScrollAmount() * (this.getHeight() - size) / this.getMaxScroll());
            Theme.scrollThumb(graphics, this.right, y, size, this.scrolling || this.isOverScrollbar(mouseX, mouseY));
        }
    }

    public void renderDebugInfo(GuiGraphicsExtractor graphics) {
        if (!SHOW_DEBUG_INFO) return;
        Minecraft client = Minecraft.getInstance();

        List<String> debugInfo = List.of(
                "T/B = " + this.top + "/" + this.bottom,
                "L/R = " + this.left + "/" + this.right,
                "S = " + this.scrolling,
                "SA/MS = " + this.getScrollAmount() + "/" + this.getMaxScroll()
        );

        RenderUtil.renderDebugInfo(graphics, client.font, this.left, this.bottom, debugInfo);
    }
    // endregion

    @Override
    public void updateNarration(NarrationElementOutput builder) {
    }

    @Override
    public List<? extends GuiEventListener> children() {
        if (!this.visible) {
            return List.of();
        }
        if (this.installedButton == null) {
            return this.children;
        }
        List<CategoryButtonWidget> all = new ArrayList<>(this.children);
        all.add(this.installedButton);
        return all;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.NONE;
    }
}
