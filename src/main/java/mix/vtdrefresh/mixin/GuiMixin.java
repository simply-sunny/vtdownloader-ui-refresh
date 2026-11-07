package mix.vtdrefresh.mixin;

import me.bymartrixx.vtd.gui.VTDownloadScreen;
import mix.vtdrefresh.gui.RefreshScreen;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Shows the refreshed screen wherever VTDownloader opens its own: the button, the pencil, and other mods. */
@Mixin(Gui.class)
abstract class GuiMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen vtdUiRefresh$replaceScreen(Screen screen) {
        if (!(screen instanceof VTDownloadScreen)) {
            return screen;
        }

        OriginalScreenAccessor original = (OriginalScreenAccessor) screen;
        PackSelectionModel.Entry pack = original.vtdUiRefresh$getPack();
        return pack != null
                ? new RefreshScreen(original.vtdUiRefresh$getParent(), original.vtdUiRefresh$getSubtitle(), pack)
                : new RefreshScreen(original.vtdUiRefresh$getParent(), original.vtdUiRefresh$getSubtitle());
    }
}
