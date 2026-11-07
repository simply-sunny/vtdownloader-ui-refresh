package mix.vtdrefresh.gui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import me.bymartrixx.vtd.VTDMod;
import mix.vtdrefresh.data.ModFiles;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** The download screen's choices that are remembered between sessions. */
public final class ScreenSettings {
    private static final String PACK_LAYOUT = "packLayout";
    private static final String SIDEBAR_WIDTH = "sidebarWidth";
    private static final String SIDEBAR_HIDDEN = "sidebarHidden";
    private static final String SELECTED_PACKS_WIDTH = "selectedPacksWidth";
    private static final String SELECTED_PACKS_HIDDEN = "selectedPacksHidden";

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static JsonObject settings;

    private ScreenSettings() {
    }

    public static PackLayout getPackLayout() {
        return settings().has(PACK_LAYOUT) && PackLayout.GRID.name().equals(settings().get(PACK_LAYOUT).getAsString())
                ? PackLayout.GRID : PackLayout.LIST;
    }

    public static void setPackLayout(PackLayout layout) {
        settings().addProperty(PACK_LAYOUT, layout.name());
        save();
    }

    /** The width the sidebar was last dragged to, or -1 if it never was. */
    public static int getSidebarWidth() {
        return getWidth(SIDEBAR_WIDTH);
    }

    public static boolean isSidebarHidden() {
        return getHidden(SIDEBAR_HIDDEN);
    }

    public static void setSidebar(int width, boolean hidden) {
        setPanel(SIDEBAR_WIDTH, width, SIDEBAR_HIDDEN, hidden);
    }

    /** The width the selected packs panel was last dragged to, or -1 if it never was. */
    public static int getSelectedPacksWidth() {
        return getWidth(SELECTED_PACKS_WIDTH);
    }

    public static boolean isSelectedPacksHidden() {
        return getHidden(SELECTED_PACKS_HIDDEN);
    }

    public static void setSelectedPacks(int width, boolean hidden) {
        setPanel(SELECTED_PACKS_WIDTH, width, SELECTED_PACKS_HIDDEN, hidden);
    }

    private static int getWidth(String key) {
        try {
            return settings().has(key) ? settings().get(key).getAsInt() : -1;
        } catch (RuntimeException e) {
            return -1;
        }
    }

    private static boolean getHidden(String key) {
        try {
            return settings().has(key) && settings().get(key).getAsBoolean();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void setPanel(String widthKey, int width, String hiddenKey, boolean hidden) {
        settings().addProperty(widthKey, width);
        settings().addProperty(hiddenKey, hidden);
        save();
    }

    private static Path file() {
        return ModFiles.dir().resolve("config.json");
    }

    private static JsonObject settings() {
        if (settings == null) {
            settings = new JsonObject();

            Path file = file();
            if (Files.exists(file)) {
                try (Reader reader = Files.newBufferedReader(file)) {
                    settings = JsonParser.parseReader(reader).getAsJsonObject();
                } catch (IOException | JsonParseException | IllegalStateException e) {
                    VTDMod.LOGGER.error("Failed to read the download screen's settings", e);
                }
            } else {
                readOldFile();
            }
        }

        return settings;
    }

    /** Older versions kept a properties file directly in {@code config}. */
    private static void readOldFile() {
        Path old = FabricLoader.getInstance().getConfigDir().resolve("vtd_ui_refresh.properties");
        if (!Files.exists(old)) return;

        Properties properties = new Properties();
        try (InputStream stream = Files.newInputStream(old)) {
            properties.load(stream);
        } catch (IOException | IllegalArgumentException e) {
            VTDMod.LOGGER.error("Failed to read the download screen's old settings", e);
            return;
        }
        if (properties.containsKey(PACK_LAYOUT)) settings.addProperty(PACK_LAYOUT, properties.getProperty(PACK_LAYOUT));
        for (String key : new String[]{SIDEBAR_WIDTH, SELECTED_PACKS_WIDTH}) {
            try {
                if (properties.containsKey(key)) settings.addProperty(key, Integer.parseInt(properties.getProperty(key)));
            } catch (NumberFormatException e) {
                // Left out, so the panel keeps its default width.
            }
        }
        for (String key : new String[]{SIDEBAR_HIDDEN, SELECTED_PACKS_HIDDEN}) {
            if (properties.containsKey(key)) settings.addProperty(key, Boolean.parseBoolean(properties.getProperty(key)));
        }
        if (save()) {
            try {
                Files.delete(old);
            } catch (IOException e) {
                VTDMod.LOGGER.warn("Failed to remove {}", old, e);
            }
        }
    }

    private static boolean save() {
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(settings(), writer);
            }
            return true;
        } catch (IOException e) {
            VTDMod.LOGGER.error("Failed to save the download screen's settings", e);
            return false;
        }
    }
}
