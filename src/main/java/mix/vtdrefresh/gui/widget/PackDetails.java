package mix.vtdrefresh.gui.widget;

import com.mojang.blaze3d.platform.NativeImage;
import me.bymartrixx.vtd.data.Category;
import me.bymartrixx.vtd.data.Pack;
import mix.vtdrefresh.data.PreviewCache;
import mix.vtdrefresh.gui.RichText;
import mix.vtdrefresh.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/**
 * A pack's page in the middle panel: its preview, playing if it's an animation, its whole description in the colours
 * Vanilla Tweaks gives it, the packs it can't be used with, and a button to select it.
 */
public class PackDetails {
    private static final Component SELECT_TEXT = Component.translatable("vtd.details.select");
    private static final Component DESELECT_TEXT = Component.translatable("vtd.details.deselect");
    private static final Component LOADING_TEXT = Component.translatable("vtd.details.loading");
    private static final Component NO_PREVIEW_TEXT = Component.translatable("vtd.details.noPreview");
    private static final Component INCOMPATIBLE_TEXT = Component.translatable("vtd.details.incompatible");

    private static final int PADDING = 8;
    private static final int ICON_SIZE = 32;
    private static final int BUTTON_HEIGHT = 16;
    private static final int BUTTON_WIDTH = 64;
    private static final int GAP = 8;
    private static final int SCROLL_STEP = 20;

    private final Minecraft client = Minecraft.getInstance();
    private final PackSelectionListWidget list;
    final PackSelectionListWidget.PackEntry entry;
    private final Component description;
    private final List<String> incompatibleNames;

    private final CompletableFuture<PreviewCache.Preview> loading;
    @Nullable
    private PreviewCache.Preview preview;
    private final List<Identifier> frames = new ArrayList<>();
    private boolean failed;
    private boolean closed;
    private long startedAt;
    private int totalDelay;

    private double scroll;
    private int contentHeight;
    private int x;
    private int y;
    private int width;
    private int height;

    PackDetails(PackSelectionListWidget list, PackSelectionListWidget.PackEntry entry, List<String> incompatibleNames) {
        this.list = list;
        this.entry = entry;
        this.description = RichText.parse(entry.getPack().getDescription());
        this.incompatibleNames = incompatibleNames;
        this.loading = PreviewCache.get(entry.getPack());
    }

    public Pack getPack() {
        return this.entry.getPack();
    }

    public double getScroll() {
        return this.scroll;
    }

    public void setScroll(double scroll) {
        this.scroll = scroll;
    }

    /** Frees the preview's textures. The page can't be shown again afterwards. */
    public void close() {
        this.closed = true;
        TextureManager textures = this.client.getTextureManager();
        this.frames.forEach(textures::release);
        this.frames.clear();
        if (this.preview != null) {
            this.preview = null;
        } else {
            // Still loading: free the frames once they arrive
            this.loading.thenAccept(preview -> {
                if (preview != null) preview.close();
            });
        }
    }

    /** Hands the decoded frames to the GPU, on the render thread, once they're ready. */
    private void uploadPreview() {
        if (this.closed || this.preview != null || this.failed || !this.loading.isDone()) {
            return;
        }

        PreviewCache.Preview preview = this.loading.getNow(null);
        if (preview == null || preview.frames().isEmpty()) {
            this.failed = true;
            return;
        }

        TextureManager textures = this.client.getTextureManager();
        String path = "preview/" + this.getPack().getId().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        for (int i = 0; i < preview.frames().size(); i++) {
            NativeImage image = preview.frames().get(i);
            Identifier id = Identifier.fromNamespaceAndPath("vtd_ui_refresh", path + "/" + i);
            textures.register(id, new DynamicTexture(() -> "VT preview " + this.getPack().getId(), image));
            this.frames.add(id);
            this.totalDelay += preview.delays()[i];
        }
        this.preview = preview;
        this.startedAt = Util.getMillis();
    }

    private Identifier currentFrame() {
        if (this.frames.size() == 1 || this.totalDelay <= 0) {
            return this.frames.getFirst();
        }

        long time = (Util.getMillis() - this.startedAt) % this.totalDelay;
        int[] delays = this.preview.delays();
        for (int i = 0; i < this.frames.size(); i++) {
            time -= delays[i];
            if (time < 0) {
                return this.frames.get(i);
            }
        }
        return this.frames.getLast();
    }

    private int contentWidth() {
        return this.width - PADDING * 2;
    }

    private int maxScroll() {
        return Math.max(0, this.contentHeight - this.height);
    }

    private int buttonX() {
        return this.x + this.width - PADDING - BUTTON_WIDTH;
    }

    private int buttonY() {
        return this.y + PADDING - (int) this.scroll + (ICON_SIZE - BUTTON_HEIGHT) / 2;
    }

    private boolean isOverButton(double mouseX, double mouseY) {
        return this.list.isEditable() && mouseX >= this.buttonX() && mouseX < this.buttonX() + BUTTON_WIDTH
                && mouseY >= this.buttonY() && mouseY < this.buttonY() + BUTTON_HEIGHT
                && mouseY >= this.y && mouseY < this.y + this.height;
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (this.isOverButton(mouseX, mouseY)) {
            this.list.toggleFromDetails(this.entry);
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double amount) {
        if (this.maxScroll() == 0) {
            return false;
        }
        this.scroll = Math.clamp(this.scroll - amount * SCROLL_STEP, 0, this.maxScroll());
        return true;
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int mouseX, int mouseY) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.uploadPreview();
        this.scroll = Math.clamp(this.scroll, 0, this.maxScroll());

        Font font = this.client.font;
        int left = x + PADDING;
        int contentWidth = this.contentWidth();
        int top = y + PADDING - (int) this.scroll;
        int cursor = top;

        graphics.enableScissor(x, y, x + width, y + height);

        // Icon, name and where the pack lives, with the select button on the right
        this.entry.renderIcon(graphics, left, cursor, ICON_SIZE);
        int textLeft = left + ICON_SIZE + GAP;
        int textWidth = this.buttonX() - GAP - textLeft;
        boolean selected = this.entry.selectionData.isSelected();
        graphics.text(font, Theme.clip(font, this.entry.getName().getString(), textWidth), textLeft, cursor + 6,
                selected ? Theme.ACCENT : Theme.WHITE, false);
        graphics.text(font, Theme.clip(font, this.categoryPath(), textWidth), textLeft, cursor + 18, Theme.FAINT, false);

        if (this.list.isEditable()) {
            boolean hovered = this.isOverButton(mouseX, mouseY);
            int bx = this.buttonX();
            int by = this.buttonY();
            int background = selected ? (hovered ? Theme.CONTROL_HOVER : Theme.CONTROL) : (hovered ? Theme.ACCENT_HOVER : Theme.ACCENT);
            Theme.box(graphics, bx, by, bx + BUTTON_WIDTH, by + BUTTON_HEIGHT, background);
            String label = (selected ? DESELECT_TEXT : SELECT_TEXT).getString();
            graphics.text(font, label, bx + (BUTTON_WIDTH - font.width(label)) / 2, by + 4,
                    selected ? Theme.WHITE : Theme.BLACK, false);
        }
        cursor += ICON_SIZE + GAP;

        // The preview, as wide as the panel allows
        cursor += this.renderPreview(graphics, font, left, cursor, contentWidth) + GAP;

        for (FormattedCharSequence line : font.split(this.description, contentWidth)) {
            graphics.text(font, line, left, cursor, Theme.DIM, false);
            cursor += font.lineHeight + 1;
        }

        if (!this.incompatibleNames.isEmpty()) {
            cursor += GAP;
            graphics.text(font, INCOMPATIBLE_TEXT.getString(), left, cursor, Theme.WHITE, false);
            cursor += font.lineHeight + 3;
            for (FormattedCharSequence line : font.split(Component.literal(String.join(", ", this.incompatibleNames)), contentWidth)) {
                graphics.text(font, line, left, cursor, Theme.DIM, false);
                cursor += font.lineHeight + 1;
            }
        }

        this.contentHeight = cursor - top + PADDING;
        graphics.disableScissor();

        if (this.maxScroll() > 0) {
            int thumb = Math.max(16, height * height / this.contentHeight);
            int thumbY = y + (int) ((height - thumb) * this.scroll / this.maxScroll());
            Theme.scrollThumb(graphics, x + width, thumbY, thumb, false);
        }
    }

    /** Draws the preview, or a box saying it's loading or missing. Returns the height used. */
    private int renderPreview(GuiGraphicsExtractor graphics, Font font, int left, int top, int width) {
        if (!this.frames.isEmpty()) {
            int drawWidth = width;
            int drawHeight = Math.round(this.preview.height() * (float) width / this.preview.width());
            graphics.blit(RenderPipelines.GUI_TEXTURED, this.currentFrame(), left, top, 0.0F, 0.0F,
                    drawWidth, drawHeight, drawWidth, drawHeight);
            return drawHeight;
        }

        int boxHeight = this.failed ? 24 : width * 9 / 16;
        Theme.box(graphics, left, top, left + width, top + boxHeight, Theme.ROW_TINT_BASE);
        Component text = this.failed ? NO_PREVIEW_TEXT : LOADING_TEXT;
        graphics.text(font, text.getString(), left + (width - font.width(text)) / 2, top + (boxHeight - 8) / 2,
                Theme.FAINT, false);
        return boxHeight;
    }

    private String categoryPath() {
        Category category = this.entry.selectionData.getCategory();
        if (category instanceof Category.SubCategory sub) {
            return sub.getParent().getName() + " / " + sub.getName();
        }
        return category.getName();
    }

}
