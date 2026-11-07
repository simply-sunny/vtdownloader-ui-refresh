package mix.vtdrefresh.data;

import me.bymartrixx.vtd.VTDMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The mod's folder in {@code config}. Everything it saves goes in here. */
public final class ModFiles {
    private ModFiles() {
    }

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve("vtd_ui_refresh");
    }

    /** Where downloaded icons and previews are kept. Moves the cache older versions left in the game folder. */
    public static synchronized Path cache() {
        Path cache = dir().resolve("cache");
        Path old = FabricLoader.getInstance().getGameDir().resolve("cache").resolve("vtd_ui_refresh");
        if (Files.isDirectory(old) && !Files.exists(cache)) {
            try {
                Files.createDirectories(dir());
                Files.move(old, cache);
            } catch (IOException e) {
                VTDMod.LOGGER.warn("Failed to move the cache from {}", old, e);
            }
        }
        return cache;
    }
}
