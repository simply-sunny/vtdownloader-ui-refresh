package mix.vtdrefresh.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** The colours and small shapes shared by every part of the download screen. */
public final class Theme {
    public static final int BLACK = 0xFF000000;
    public static final int PANEL = 0xFF121212;
    public static final int ROW_HOVER = 0xFF2A2A2A;
    public static final int SELECTED = 0xFF232323;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int DIM = 0xFFB3B3B3;
    public static final int FAINT = 0xFF6A6A6A;
    // The theme colour of vanillatweaks.net
    public static final int ACCENT = 0xFFE99743;
    public static final int ACCENT_HOVER = 0xFFF2B070;
    public static final int RED = 0xFFFF6B6B;
    public static final int TRACK_BG = 0xFF4D4D4D;
    public static final int CONTROL = 0xFF2A2A2A;
    public static final int CONTROL_HOVER = 0xFF3A3A3A;
    public static final int CONTROL_DISABLED = 0xFF1C1C1C;
    public static final int MENU_BG = 0xFF282828;
    public static final int ROW_TINT_BASE = 0xFF1C1C1C;

    private static final String ELLIPSIS = "...";

    private Theme() {
    }

    /** A filled box with each corner pixel snipped off, the one shape for buttons and cards. */
    public static void box(GuiGraphicsExtractor graphics, int x0, int y0, int x1, int y1, int color) {
        graphics.fill(x0 + 1, y0, x1 - 1, y1, color);
        graphics.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        graphics.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    /** {@code text} cut to fit {@code width}, ending in an ellipsis when anything was cut. */
    public static String clip(Font font, String text, int width) {
        if (font.width(text) <= width) {
            return text;
        }

        int room = Math.max(0, width - font.width(ELLIPSIS));
        return font.plainSubstrByWidth(text, room).stripTrailing() + ELLIPSIS;
    }

    /** An opaque colour {@code amount} of the way from {@code from} to {@code to}. */
    public static int mix(int from, int to, float amount) {
        int r = channel(from >> 16, to >> 16, amount);
        int g = channel(from >> 8, to >> 8, amount);
        int b = channel(from, to, amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int channel(int from, int to, float amount) {
        int a = from & 0xFF;
        int b = to & 0xFF;
        return Math.round(a + (b - a) * amount);
    }

    public static int withAlpha(int color, int alpha) {
        return color & 0x00FFFFFF | alpha << 24;
    }

    /** An arrow pointing right, for rows and boxes that open something. Each string is a row, 'X' a pixel. */
    public static final String[] ARROW = {
            "...X..",
            "....X.",
            "XXXXXX",
            "....X.",
            "...X..",
    };
    public static final int ARROW_BUTTON_SIZE = 13;

    /** Draws a pixel icon, each string a row where 'X' is a filled pixel. */
    public static void icon(GuiGraphicsExtractor graphics, String[] icon, int x, int y, int color) {
        for (int row = 0; row < icon.length; row++) {
            for (int column = 0; column < icon[row].length(); column++) {
                if (icon[row].charAt(column) == 'X') {
                    graphics.fill(x + column, y + row, x + column + 1, y + row + 1, color);
                }
            }
        }
    }

    /**
     * The small button that opens a pack's details or a subcategory. Quiet until its row is hovered, and orange
     * while the pointer is on the button itself.
     */
    public static void arrowButton(GuiGraphicsExtractor graphics, int x, int y, boolean rowHovered, boolean hovered) {
        int size = ARROW_BUTTON_SIZE;
        if (hovered) {
            box(graphics, x, y, x + size, y + size, ACCENT);
        } else if (rowHovered) {
            box(graphics, x, y, x + size, y + size, CONTROL_HOVER);
        }
        int color = hovered ? BLACK : rowHovered ? WHITE : FAINT;
        icon(graphics, ARROW, x + (size - ARROW[0].length()) / 2, y + (size - ARROW.length) / 2, color);
    }

    public static boolean isOver(double mouseX, double mouseY, int x, int y, int size) {
        return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
    }

    /** A thin scrollbar thumb, without a track behind it. */
    public static void scrollThumb(GuiGraphicsExtractor graphics, int right, int y, int height, boolean hovered) {
        graphics.fill(right - 3, y, right - 1, y + height, hovered ? FAINT : TRACK_BG);
    }
}
