package mix.vtdrefresh.gui.widget;

import net.minecraft.network.chat.Component;

public class MutableMessageButtonWidget extends FlatButtonWidget {
    private final Component defaultMessage;
    private Component currentMessage;

    public MutableMessageButtonWidget(int x, int y, int width, int height, Component message, OnPress onPress, Style style) {
        super(x, y, width, height, message, onPress, style);
        this.defaultMessage = message;
        this.currentMessage = message;
    }

    public void resetMessage() {
        this.currentMessage = this.defaultMessage;
    }

    public void setMessage(Component message) {
        this.currentMessage = message;
    }

    @Override
    public Component getMessage() {
        return this.currentMessage;
    }
}
