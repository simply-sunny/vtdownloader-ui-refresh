package mix.vtdrefresh.mixin;

import me.bymartrixx.vtd.gui.VTDownloadScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.packs.PackSelectionModel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** What VTDownloader's own screen was opened with, to open the refreshed one the same way. */
@Mixin(VTDownloadScreen.class)
public interface OriginalScreenAccessor {
    @Accessor("parent")
    Screen vtdUiRefresh$getParent();

    @Accessor("subtitle")
    Component vtdUiRefresh$getSubtitle();

    @Accessor("pack")
    @Nullable
    PackSelectionModel.Entry vtdUiRefresh$getPack();
}
