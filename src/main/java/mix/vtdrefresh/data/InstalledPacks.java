package mix.vtdrefresh.data;

import me.bymartrixx.vtd.VTDMod;
import me.bymartrixx.vtd.util.Constants;
import net.minecraft.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Finds the Vanilla Tweaks packs in the resource pack folder, from the list each one keeps of its packs. */
public final class InstalledPacks {
    /** The pack ids in each zip or folder, by its file name, in name order. */
    public record Result(Map<String, List<String>> bySource) {
        public static final Result EMPTY = new Result(Map.of());
    }

    private InstalledPacks() {
    }

    public static CompletableFuture<Result> scan(Path directory) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, List<String>> bySource = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            if (!Files.isDirectory(directory)) {
                return Result.EMPTY;
            }

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
                for (Path path : stream) {
                    List<String> found = read(path);
                    if (!found.isEmpty()) {
                        bySource.put(path.getFileName().toString(), found);
                    }
                }
            } catch (IOException e) {
                VTDMod.LOGGER.warn("Couldn't list the resource pack folder", e);
            }

            return new Result(new LinkedHashMap<>(bySource));
        }, Util.ioPool());
    }

    /** The pack ids in one zip or folder, empty if it isn't a Vanilla Tweaks pack. */
    public static List<String> read(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Path file = path.resolve(Constants.SELECTED_PACKS_FILE);
                if (Files.isRegularFile(file)) {
                    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                        return parse(reader);
                    }
                }
            } else if (path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".zip")) {
                try (ZipFile zip = new ZipFile(path.toFile())) {
                    ZipEntry entry = zip.getEntry(Constants.SELECTED_PACKS_FILE);
                    if (entry != null) {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(zip.getInputStream(entry), StandardCharsets.UTF_8))) {
                            return parse(reader);
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            VTDMod.LOGGER.debug("Skipped {}, which isn't a readable Vanilla Tweaks pack", path, e);
        }

        return List.of();
    }

    /**
     * Reads the pack ids under "Packs:". Unlike VTDownloader's reader this takes any Vanilla Tweaks version,
     * as a pack made for another one still lists ids that mostly exist; the screen skips the ones that don't.
     */
    static List<String> parse(BufferedReader reader) throws IOException {
        String first = reader.readLine();
        if (first == null || !first.trim().equals(Constants.SELECTED_PACKS_FILE_HEADER)) {
            return List.of();
        }

        List<String> ids = new ArrayList<>();
        boolean inPacks = false;
        String line;
        while ((line = reader.readLine()) != null) {
            String trimmed = line.trim();
            if (inPacks) {
                if (!trimmed.isEmpty()) {
                    ids.add(trimmed);
                }
            } else if (trimmed.startsWith("Packs:")) {
                inPacks = true;
            }
        }

        return ids;
    }
}
