package mix.vtdrefresh.command;

import me.bymartrixx.vtd.VTDMod;
import me.bymartrixx.vtd.data.Category;
import me.bymartrixx.vtd.data.DownloadPackRequestData;
import me.bymartrixx.vtd.data.Pack;
import me.bymartrixx.vtd.util.Constants;
import mix.vtdrefresh.data.InstalledPacks;
import mix.vtdrefresh.data.ModFiles;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** Makes, changes and turns on Vanilla Tweaks zips for the {@code /vtd} commands. Runs on the game's thread. */
final class CommandPacks {
    private static final String PREFIX = "vtd.command.";

    /** Where a zip is told to go, and where its messages go. */
    record Target(String name, Consumer<Component> feedback, Consumer<Component> error) {
        String fileName() {
            return this.name + ".zip";
        }

        void say(String key, Object... args) {
            this.feedback.accept(Component.translatable(PREFIX + key, args));
        }

        void fail(String key, Object... args) {
            this.error.accept(Component.translatable(PREFIX + key, args));
        }
    }

    private static boolean busy;

    private CommandPacks() {
    }

    static Path folder() {
        return Minecraft.getInstance().getResourcePackDirectory();
    }

    static boolean ready() {
        return VTDMod.rpCategories != null && !VTDMod.rpCategories.getCategories().isEmpty();
    }

    /** The Vanilla Tweaks zips in the folder, by name without {@code .zip}. */
    static List<String> zips() {
        try (Stream<Path> files = Files.list(folder())) {
            return files.map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".zip"))
                    .filter(name -> !InstalledPacks.read(folder().resolve(name)).isEmpty())
                    .map(name -> name.substring(0, name.length() - 4))
                    .sorted(String.CASE_INSENSITIVE_ORDER).toList();
        } catch (IOException e) {
            VTDMod.LOGGER.warn("Couldn't list the resource pack folder", e);
            return List.of();
        }
    }

    static List<String> read(String zip) {
        return InstalledPacks.read(folder().resolve(zip + ".zip"));
    }

    static List<String> allPackIds() {
        if (!ready()) return List.of();
        List<String> ids = new ArrayList<>();
        for (Category category : VTDMod.rpCategories.getCategories()) {
            category.getPacks().forEach(pack -> ids.add(pack.getId()));
            if (category.getSubCategories() != null) {
                category.getSubCategories().forEach(sub -> sub.getPacks().forEach(pack -> ids.add(pack.getId())));
            }
        }
        return ids.stream().distinct().toList();
    }

    /** Matches whatever case was typed. Null if there's no such pack. */
    private static String findId(String typed) {
        return allPackIds().stream().filter(id -> id.equalsIgnoreCase(typed)).findFirst().orElse(null);
    }

    static boolean isOn(String zip) {
        return Minecraft.getInstance().getResourcePackRepository().getSelectedIds().contains(repoId(zip));
    }

    private static String repoId(String zip) {
        return "file/" + zip + ".zip";
    }

    private static boolean validName(String name) {
        return !name.isBlank() && !name.startsWith(".") && name.chars().noneMatch(c -> "/\\:*?\"<>|".indexOf(c) >= 0);
    }

    static void list(Target target) {
        List<String> zips = zips();
        if (zips.isEmpty()) target.say("no_zips");
        for (String zip : zips) {
            target.say(isOn(zip) ? "zip_on" : "zip_off", zip, read(zip).size());
        }
    }

    static void show(Target target) {
        List<String> ids = read(target.name());
        if (ids.isEmpty()) {
            target.fail("missing", target.name());
            return;
        }
        target.feedback().accept(Component.literal(String.join(", ", ids)));
    }

    static void create(Target target, List<String> typed, boolean reload) {
        if (!canStart(target)) return;
        if (!validName(target.name())) {
            target.fail("bad_name");
        } else if (Files.exists(folder().resolve(target.fileName()))) {
            target.fail("exists", target.name());
        } else {
            List<String> ids = resolve(target, typed);
            if (ids != null) build(target, new LinkedHashSet<>(ids), reload);
        }
    }

    static void add(Target target, List<String> typed, boolean reload) {
        if (!canStart(target)) return;
        List<String> current = read(target.name());
        if (current.isEmpty()) {
            target.fail("missing", target.name());
            return;
        }
        List<String> ids = resolve(target, typed);
        if (ids == null) return;

        Set<String> next = new LinkedHashSet<>(current);
        next.addAll(ids);
        if (next.size() == current.size()) target.say("nothing");
        else build(target, next, reload);
    }

    static void remove(Target target, List<String> typed, boolean reload) {
        if (!canStart(target)) return;
        List<String> current = read(target.name());
        if (current.isEmpty()) {
            target.fail("missing", target.name());
            return;
        }

        Set<String> next = new LinkedHashSet<>(current);
        // A pack from an older Vanilla Tweaks version can be removed by name, though it no longer exists.
        typed.forEach(id -> next.removeIf(id::equalsIgnoreCase));
        if (next.isEmpty()) target.fail("empty", target.name());
        else if (next.size() == current.size()) target.say("nothing");
        else build(target, next, reload);
    }

    static void setOn(Target target, boolean on, boolean reload) {
        if (read(target.name()).isEmpty()) {
            target.fail("missing", target.name());
            return;
        }
        select(target.name(), on);
        if (reload) {
            target.say("reloaded");
            Minecraft.getInstance().reloadResourcePacks();
        } else {
            target.say(on ? "loaded" : "unloaded", target.name());
        }
    }

    private static boolean canStart(Target target) {
        if (busy) {
            target.fail("busy");
        } else if (!ready()) {
            target.fail("not_ready");
        } else {
            return true;
        }
        return false;
    }

    /** The ids of what was typed, or null after saying what was wrong. */
    private static List<String> resolve(Target target, List<String> typed) {
        if (typed.isEmpty()) {
            target.fail("none");
            return null;
        }
        List<String> ids = new ArrayList<>(), unknown = new ArrayList<>();
        for (String word : typed) {
            String id = findId(word);
            if (id != null) ids.add(id);
            else unknown.add(word);
        }
        if (!unknown.isEmpty()) {
            target.fail("unknown", String.join(", ", unknown));
            return null;
        }
        return ids;
    }

    /** Asks Vanilla Tweaks for the zip, which comes back into the cache, then puts it in place. */
    private static void build(Target target, Set<String> ids, boolean reload) {
        Map<Category, List<Pack>> packs = new LinkedHashMap<>();
        int dropped = 0;
        for (String id : ids) {
            Pack pack = VTDMod.rpCategories.findPack(id);
            if (pack == null) dropped++;
            else packs.computeIfAbsent(VTDMod.rpCategories.getCategory(pack), c -> new ArrayList<>()).add(pack);
        }
        warnConflicts(target, packs);
        if (dropped > 0) target.say("dropped", dropped);

        Path cache = ModFiles.cache().resolve("commands");
        try {
            Files.createDirectories(cache);
        } catch (IOException e) {
            failed(target, e);
            return;
        }

        busy = true;
        target.say("downloading", target.name());
        Minecraft minecraft = Minecraft.getInstance();
        int count = packs.values().stream().mapToInt(List::size).sum();
        VTDMod.executePackDownload(DownloadPackRequestData.create(packs), f -> {}, cache, target.name())
                .completeOnTimeout(false, Constants.PACK_DOWNLOAD_TIMEOUT, TimeUnit.SECONDS)
                .whenCompleteAsync((success, throwable) -> {
                    busy = false;
                    if (throwable != null || !success) {
                        VTDMod.LOGGER.error("Pack download failed", throwable);
                        target.fail("failed", target.name(), throwable != null ? throwable.getLocalizedMessage() : "timed out");
                    } else {
                        place(target, cache.resolve(target.fileName()), count, reload);
                    }
                }, minecraft);
    }

    private static void warnConflicts(Target target, Map<Category, List<Pack>> packs) {
        List<Pack> all = packs.values().stream().flatMap(List::stream).toList();
        for (int i = 0; i < all.size(); i++) {
            for (Pack other : all.subList(i + 1, all.size())) {
                if (!all.get(i).isCompatible(other)) target.say("conflict", all.get(i).getName(), other.getName());
            }
        }
    }

    private static void place(Target target, Path made, int count, boolean reload) {
        Path to = folder().resolve(target.fileName());
        Minecraft minecraft = Minecraft.getInstance();
        try {
            Files.move(made, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (FileSystemException e) {
            // Windows won't replace a zip the game has open.
            if (!reload || !isOn(target.name())) {
                target.fail("in_use", target.name());
                return;
            }
            select(target.name(), false);
            minecraft.reloadResourcePacks().thenRunAsync(() -> {
                try {
                    Files.move(made, to, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e2) {
                    failed(target, e2);
                }
                finish(target, count);
            }, minecraft);
            return;
        } catch (IOException e) {
            failed(target, e);
            return;
        }

        if (reload) finish(target, count);
        else target.say(isOn(target.name()) ? "saved_on" : "saved", target.name(), count);
    }

    private static void finish(Target target, int count) {
        select(target.name(), true);
        Minecraft.getInstance().reloadResourcePacks();
        target.say("applied", target.name(), count);
    }

    private static void failed(Target target, Exception e) {
        VTDMod.LOGGER.error("Couldn't save {}", target.fileName(), e);
        target.fail("failed", target.name(), e.getLocalizedMessage());
    }

    /** Turns a zip on or off and saves that to options.txt, without reloading. */
    private static void select(String zip, boolean on) {
        Minecraft minecraft = Minecraft.getInstance();
        var repository = minecraft.getResourcePackRepository();
        repository.reload();
        if (on) repository.addPack(repoId(zip));
        else repository.removePack(repoId(zip));

        var options = minecraft.options;
        options.resourcePacks.clear();
        options.incompatibleResourcePacks.clear();
        for (var pack : repository.getSelectedPacks()) {
            if (pack.isFixedPosition()) continue;
            options.resourcePacks.add(pack.getId());
            if (!pack.getCompatibility().isCompatible()) options.incompatibleResourcePacks.add(pack.getId());
        }
        options.save();
    }
}
