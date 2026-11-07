package mix.vtdrefresh.gui.popup;

import mix.vtdrefresh.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.function.Supplier;

public class ProgressBarScreenPopup extends AbstractScreenPopup {
    private static final int BAR_HEIGHT = 4;
    private static final int BAR_MARGIN = 10;

    private final int color;
    private Supplier<Float> progress = () -> 0.0F;
    private Runnable onReset;

    private boolean aborted;

    public ProgressBarScreenPopup(Minecraft client, int centerX, int centerY, int width, int height, int color) {
        super(client, centerX, centerY, width, height);

        this.color = color;
    }

    public void show(float time, Supplier<Float> progress, Runnable onReset) {
        this.show(time);

        this.progress = progress;
        this.onReset = onReset;
    }

    public void abortWait() {
        this.aborted = true;
    }

    private int getBarWidth() {
        return this.getWidth() - BAR_MARGIN * 2;
    }

    @Override
    protected boolean shouldUpdateTime() {
        return this.aborted || this.progress.get() >= 1.0F;
    }

    @Override
    protected void reset() {
        this.aborted = false;
        this.onReset.run();
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        float p = this.progress.get();
        if (p < 0.0F) {
            return;
        }

        int barWidth = this.getBarWidth();
        int progressWidth = Math.round(barWidth * p);
        int x = this.centerX - barWidth / 2;
        int y = this.centerY - BAR_HEIGHT / 2;
        int alpha = this.getFadeAlpha();

        graphics.fill(x, y, x + barWidth, y + BAR_HEIGHT, Theme.withAlpha(Theme.TRACK_BG, alpha));
        graphics.fill(x, y, x + progressWidth, y + BAR_HEIGHT, Theme.withAlpha(this.color, alpha));
    }
}
