package mix.vtdrefresh.gui.widget;

import me.bymartrixx.vtd.data.Category;
import mix.vtdrefresh.gui.Theme;
import mix.vtdrefresh.gui.RefreshScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import com.mojang.blaze3d.platform.InputConstants;

// Doesn't extend ButtonWidget to allow dynamic positioning
public class CategoryButtonWidget implements GuiEventListener, NarratableEntry {
    private static final int TEXT_LEFT = 6;
    private static final int INDENT = 10;

    private final Category category;
    private final int height;
    private final Component text;
    private final RefreshScreen screen;
    private boolean selected = false;
    /** A top-level category whose subcategory is shown: its name is marked, without the selected fill. */
    private boolean open = false;
    private int depth = 0;
    private boolean hovered;
    private boolean focused;

    public CategoryButtonWidget(RefreshScreen screen, int height, Component text, Category category) {
        this.screen = screen;
        this.height = height;
        this.text = text;
        this.category = category;
    }

    public void render(GuiGraphicsExtractor graphics, int x, int y, int width, int mouseX, int mouseY) {
        this.hovered = mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + this.height;
        this.renderButton(graphics, x, y, width);
    }

    /** For a row scrolled out of view, which must not take clicks meant for what's there now. */
    public void clearHover() {
        this.hovered = false;
    }

    private void renderButton(GuiGraphicsExtractor graphics, int x, int y, int width) {
        Font font = Minecraft.getInstance().font;

        if (this.selected || this.isHoveredOrFocused()) {
            graphics.fill(x, y, x + width, y + this.height, this.selected ? Theme.SELECTED : Theme.ROW_HOVER);
        }

        int color = this.selected || this.open ? Theme.ACCENT : this.isHoveredOrFocused() ? Theme.WHITE : Theme.DIM;
        int left = TEXT_LEFT + this.depth * INDENT;
        String label = Theme.clip(font, this.text.getString(), width - left - TEXT_LEFT);
        graphics.text(font, label, x + left, y + (this.height - 8) / 2, color, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && this.hovered && !this.selected) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return this.screen.selectCategory(this.category);
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (this.selected) {
            return false;
        }

        if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_NUMPADENTER) {
            this.playDownSound(Minecraft.getInstance().getSoundManager());
            return this.screen.selectCategory(this.category);
        }

        return GuiEventListener.super.keyPressed(event);
    }

    // TODO
    @Override
    public void setFocused(boolean focused) {
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.hovered;
    }

    private void playDownSound(SoundManager soundManager) {
        soundManager.play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarrationPriority.FOCUSED;
        } else if (this.hovered) {
            return NarrationPriority.HOVERED;
        }

        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        builder.add(NarratedElementType.TITLE, AbstractWidget.wrapDefaultNarrationMessage(this.text));
        if (!this.selected) {
            if (this.focused) {
                builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
            } else {
                builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
            }
        }
    }

    private boolean isHoveredOrFocused() {
        return this.hovered || this.focused;
    }

    public boolean isSelected() {
        return this.selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public Category getCategory() {
        return this.category;
    }
}
