package mix.vtdrefresh.gui.widget;

import net.minecraft.network.chat.Component;

public class DebugButtonWidget extends FlatButtonWidget {
    public DebugButtonWidget(int x, int y, Component message, OnPress onPress) {
        super(x, y, IconButtonWidget.BUTTON_SIZE, IconButtonWidget.BUTTON_SIZE, message, onPress, Style.SECONDARY);
    }

    @Override
    protected String getLabel() {
        return "D";
    }
}
