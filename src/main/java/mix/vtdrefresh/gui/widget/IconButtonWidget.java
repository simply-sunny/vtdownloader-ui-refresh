package mix.vtdrefresh.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

/** A square button showing an icon drawn pixel by pixel, each row a string where 'X' is a filled pixel. */
public class IconButtonWidget extends FlatButtonWidget {
    public static final int BUTTON_SIZE = 15;

    private final String[] icon;

    public IconButtonWidget(int x, int y, String[] icon, Component message, OnPress onPress) {
        this(x, y, BUTTON_SIZE, BUTTON_SIZE, icon, message, onPress);
    }

    public IconButtonWidget(int x, int y, int width, int height, String[] icon, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, Style.SECONDARY);
        this.icon = icon;
        this.setTooltip(Tooltip.create(message));
    }

    @Override
    protected void extractForeground(GuiGraphicsExtractor graphics) {
        int left = this.getX() + (this.width - this.icon[0].length()) / 2;
        int top = this.getY() + (this.height - this.icon.length) / 2;
        int color = this.textColor();

        for (int row = 0; row < this.icon.length; row++) {
            for (int column = 0; column < this.icon[row].length(); column++) {
                if (this.icon[row].charAt(column) == 'X') {
                    graphics.fill(left + column, top + row, left + column + 1, top + row + 1, color);
                }
            }
        }
    }
}
