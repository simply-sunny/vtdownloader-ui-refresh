package mix.vtdrefresh.gui.widget;

import net.minecraft.network.chat.Component;

public class ReloadButtonWidget extends IconButtonWidget {
    // A clockwise arrow
    private static final String[] ICON = {
            "..XXXX..X",
            ".X....X.X",
            "X......XX",
            "X....XXXX",
            "X........",
            "X.......X",
            "X.......X",
            ".X.....X.",
            "..XXXXX.."
    };

    public ReloadButtonWidget(int x, int y, Component message, OnPress onPress) {
        super(x, y, ICON, message, onPress);
    }
}
