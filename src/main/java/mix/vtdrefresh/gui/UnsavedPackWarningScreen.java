package mix.vtdrefresh.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import mix.vtdrefresh.gui.widget.FlatButtonWidget;
import me.bymartrixx.vtd.util.Util;

public class UnsavedPackWarningScreen extends Screen {
    private static final Component HEADER = Component.translatable("vtd.unsavedPackWarning.header");
    private static final Component MESSAGE = Component.translatable("vtd.unsavedPackWarning.text");

    private static final int CARD_WIDTH = 324;
    private static final int CARD_ABOVE = 62;
    private static final int CARD_BELOW = 72;

    private final RefreshScreen parent;
    private final Screen next;
    private MultiLineLabel message;

    protected UnsavedPackWarningScreen(RefreshScreen parent, Screen next) {
        super(HEADER);
        this.parent = parent;
        this.next = next;
    }

    @Override
    protected void init() {
        this.message = Util.createMultilineText(this.font, MESSAGE, 10, 300);

        int buttonWidth = 150;
        int buttonHeight = 20;
        int spacing = 8;
        int totalWidth = buttonWidth * 2 + spacing;
        int startX = (this.width - totalWidth) / 2;
        int buttonY = this.height / 2 + 40;

        this.addRenderableWidget(new FlatButtonWidget(startX, buttonY, buttonWidth, buttonHeight, CommonComponents.GUI_PROCEED,
                button -> this.minecraft.gui.setScreen(this.next), FlatButtonWidget.Style.SECONDARY));

        this.addRenderableWidget(new FlatButtonWidget(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight, CommonComponents.GUI_BACK,
                button -> this.onClose(), FlatButtonWidget.Style.PRIMARY));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, Theme.BLACK);

        int left = (this.width - CARD_WIDTH) / 2;
        Theme.box(graphics, left, this.height / 2 - CARD_ABOVE, left + CARD_WIDTH, this.height / 2 + CARD_BELOW, Theme.PANEL);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int headerWidth = this.font.width(HEADER);
        graphics.text(this.font, HEADER, (this.width - headerWidth) / 2, this.height / 2 - 50, Theme.RED, false);

        int y = this.height / 2 - 20;
        TextAlignment alignment = TextAlignment.CENTER;
        this.message.visitLines(alignment, this.width / 2, y, this.font.lineHeight, graphics.textRenderer());
    }
}
