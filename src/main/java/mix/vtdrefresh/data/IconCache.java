package mix.vtdrefresh.data;

import com.mojang.blaze3d.platform.NativeImage;
import me.bymartrixx.vtd.VTDMod;
import me.bymartrixx.vtd.data.Pack;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Keeps the pack icons on disk, so they're downloaded once rather than every time the game starts.
 * They're kept per Vanilla Tweaks version, which is when the icons can change.
 */
public final class IconCache {
    private static final Path DIRECTORY = ModFiles.cache().resolve("icons");

    private IconCache() {
    }

    private static Path getPath(Pack pack) {
        // The icon name comes from the pack list, so keep it to a plain file name
        String name = pack.getIcon().replaceAll("[^A-Za-z0-9_.-]", "_");
        return DIRECTORY.resolve(VTDMod.VT_VERSION.replaceAll("[^A-Za-z0-9_.-]", "_")).resolve(name + ".png");
    }

    /** The cached icon if there is one, otherwise a download that's saved for next time. */
    public static CompletableFuture<NativeImage> get(Pack pack) {
        Path path = getPath(pack);
        return CompletableFuture.supplyAsync(() -> read(path), Util.ioPool()).thenCompose(cached -> {
            if (cached != null) {
                return CompletableFuture.completedFuture(cached);
            }

            return VTDMod.downloadIcon(pack).thenApplyAsync(icon -> {
                write(path, icon);
                return icon;
            }, Util.ioPool());
        });
    }

    private static NativeImage read(Path path) {
        if (!Files.isRegularFile(path)) {
            return null;
        }

        try (InputStream stream = Files.newInputStream(path)) {
            return NativeImage.read(stream);
        } catch (IOException e) {
            // A broken file is downloaded again
            VTDMod.LOGGER.warn("Couldn't read the cached icon {}", path, e);
            return null;
        }
    }

    private static void write(Path path, NativeImage icon) {
        if (icon == null) {
            return;
        }

        try {
            Files.createDirectories(path.getParent());
            icon.writeToFile(path);
        } catch (IOException e) {
            VTDMod.LOGGER.warn("Couldn't cache the icon {}", path, e);
        }
    }
}
