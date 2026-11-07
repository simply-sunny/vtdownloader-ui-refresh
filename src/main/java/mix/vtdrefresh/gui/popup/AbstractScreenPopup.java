package mix.vtdrefresh.gui.popup;

import mix.vtdrefresh.gui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.util.ARGB;

public abstract class AbstractScreenPopup implements Renderable {
    private static final int SHADOW_OFFSET = 3;
    private static final int SHADOW_ALPHA = 0x66;
    private static final float FADE_TIME = 20.0F;

    protected final Minecraft client;
    protected final int centerX;
    protected final int centerY;
    private int width;
    private int height;

    private boolean show;
    private float shownTime;
    private float fadeTime;

    public AbstractScreenPopup(Minecraft client, int centerX, int centerY, int width, int height) {
        this.client = client;
        this.centerX = centerX;
        this.centerY = centerY;
        this.width = width;
        this.height = height;
    }

    protected void show(float time) {
        this.show = true;
        this.shownTime = time;
        this.fadeTime = 0.0F;
    }

    public boolean shouldShow() {
        return this.show;
    }

    protected final int getLeft() {
        return this.centerX - this.width / 2;
    }

    protected final int getRight() {
        return this.getLeft() + this.width;
    }

    protected final int getTop() {
        return this.centerY - this.height / 2;
    }

    protected final int getBottom() {
        return this.getTop() + this.height;
    }

    protected final int getWidth() {
        return this.width;
    }

    protected final int getHeight() {
        return this.height;
    }

    protected final float getFadeOpacity() {
        return Math.clamp((FADE_TIME - this.fadeTime) / FADE_TIME, 0.0f, 1.0f);
    }

    protected final int getFadeAlpha() {
        return ARGB.as8BitChannel(this.getFadeOpacity());
    }

    protected void updateSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.show && mouseX >= this.getLeft() && mouseX < this.getRight()
                && mouseY >= this.getTop() && mouseY < this.getBottom();
    }

    protected void updateShownTime(float delta) {
        if (this.shouldUpdateTime() && this.show) {
            if (this.shownTime > 0.0F) {
                this.shownTime -= delta;
                if (this.shownTime <= 0.0F) {
                    this.shownTime = 0.0F;
                    this.fadeTime = 0.0F;
                }
            } else if (this.fadeTime >= 0.0F) {
                this.fadeTime += delta;
                if (this.fadeTime >= FADE_TIME) {
                    this.fadeTime = 0.0F;
                    this.show = false;
                    this.reset();
                }
            }
        }
    }

    protected boolean shouldUpdateTime() {
        return true;
    }

    protected void reset() {}

    @Override
    public final void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (this.show) {
            // Above the lists, whose text would otherwise be drawn over the box
            graphics.nextStratum();
            this.renderBackground(graphics);
            this.renderContent(graphics, mouseX, mouseY, delta);
            this.updateShownTime(delta);
        }
    }

    protected void renderBackground(GuiGraphicsExtractor graphics) {
        int alpha = this.getFadeAlpha();
        graphics.fill(this.getLeft() + 2, this.getTop() + SHADOW_OFFSET, this.getRight() + SHADOW_OFFSET, this.getBottom() + SHADOW_OFFSET,
                Theme.withAlpha(Theme.BLACK, SHADOW_ALPHA * alpha / 255));
        Theme.box(graphics, this.getLeft(), this.getTop(), this.getRight(), this.getBottom(), Theme.withAlpha(Theme.MENU_BG, alpha));
    }

    protected abstract void renderContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta);
}
