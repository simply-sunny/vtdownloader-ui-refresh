package mix.vtdrefresh.gui.widget;

import mix.vtdrefresh.gui.Theme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/** A text field in the screen's style, drawn as a filled box or as a line, with a placeholder shown while it's empty. */
public class FlatTextFieldWidget extends EditBox {
    private static final int PADDING = 6;
    private static final int TEXT_HEIGHT = 8;
    /** The height of a capital letter, what looks centered: the rest of the text's height is only used by descenders. */
    private static final int CAPITAL_HEIGHT = 7;

    private final Font font;
    private final boolean underlined;
    private final int padding;
    protected int boxLeft;
    protected int boxTop;
    protected int boxRight;
    protected int boxBottom;

    public FlatTextFieldWidget(Font font, int x, int y, int width, int height, Component placeholder) {
        this(font, x, y, width, height, placeholder, false);
    }

    public FlatTextFieldWidget(Font font, int x, int y, int width, int height, Component placeholder, boolean underlined) {
        // Unbordered, so that the box can be drawn in the screen's style: the box is the given area, the text inside it
        super(font, x + (underlined ? 0 : PADDING), y + (height - CAPITAL_HEIGHT) / 2,
                width - (underlined ? 0 : PADDING * 2), TEXT_HEIGHT, placeholder);
        this.font = font;
        this.underlined = underlined;
        this.padding = underlined ? 0 : PADDING;
        this.setBordered(false);
        this.setTextShadow(false);
        this.boxLeft = x;
        this.boxTop = y;
        this.boxRight = x + width;
        this.boxBottom = y + height;
    }

    public void setBoxBounds(int x, int width) {
        this.setX(x + this.padding);
        this.setWidth(width - this.padding * 2);
        this.boxLeft = x;
        this.boxRight = x + width;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.isActive() && mouseX >= this.boxLeft && mouseX < this.boxRight
                && mouseY >= this.boxTop && mouseY < this.boxBottom;
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (!this.isVisible()) {
            return;
        }

        boolean light = this.isFocused() || this.isMouseOver(mouseX, mouseY);
        if (this.underlined) {
            graphics.fill(this.boxLeft, this.boxBottom - 1, this.boxRight, this.boxBottom,
                    this.isFocused() ? Theme.ACCENT : light ? Theme.FAINT : Theme.TRACK_BG);
        } else {
            Theme.box(graphics, this.boxLeft, this.boxTop, this.boxRight, this.boxBottom, light ? Theme.CONTROL_HOVER : Theme.CONTROL);
        }

        super.extractWidgetRenderState(graphics, mouseX, mouseY, delta);

        if (this.getValue().isEmpty()) {
            String placeholder = Theme.clip(this.font, this.getMessage().getString(), this.width);
            graphics.text(this.font, placeholder, this.getX(), this.getY(), Theme.FAINT, false);
        }
    }
}
