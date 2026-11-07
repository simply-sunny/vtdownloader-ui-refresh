package mix.vtdrefresh.gui.widget;

import mix.vtdrefresh.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class FlatButtonWidget extends Button {
    public enum Style {
        PRIMARY,
        SECONDARY
    }

    private Style style;

    public FlatButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.style = style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    private int backgroundColor() {
        if (!this.active) {
            return Theme.CONTROL_DISABLED;
        }

        if (this.style == Style.PRIMARY) {
            return this.isHoveredOrFocused() ? Theme.ACCENT_HOVER : Theme.ACCENT;
        }

        return this.isHoveredOrFocused() ? Theme.CONTROL_HOVER : Theme.CONTROL;
    }

    protected int textColor() {
        if (!this.active) {
            return Theme.FAINT;
        }

        return this.style == Style.PRIMARY ? Theme.BLACK : Theme.WHITE;
    }

    protected String getLabel() {
        return this.getMessage().getString();
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Theme.box(graphics, this.getX(), this.getY(), this.getRight(), this.getBottom(), this.backgroundColor());
        this.extractForeground(graphics);
    }

    protected void extractForeground(GuiGraphicsExtractor graphics) {
        Font font = Minecraft.getInstance().font;
        String label = Theme.clip(font, this.getLabel(), this.width - 6);
        int textX = this.getX() + (this.width - font.width(label)) / 2;
        int textY = this.getY() + (this.height - 8) / 2;
        graphics.text(font, label, textX, textY, this.textColor(), false);
    }
}
