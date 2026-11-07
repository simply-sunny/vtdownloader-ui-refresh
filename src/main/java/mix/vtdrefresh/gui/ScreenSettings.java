package mix.vtdrefresh.gui;

import me.bymartrixx.vtd.VTDMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    private static Properties properties;

    private ScreenSettings() {
    }

    public static PackLayout getPackLayout() {
        return PackLayout.GRID.name().equals(properties().getProperty(PACK_LAYOUT)) ? PackLayout.GRID : PackLayout.LIST;
    }

    public static void setPackLayout(PackLayout layout) {
        properties().setProperty(PACK_LAYOUT, layout.name());
        save();
    }

    /** The width the sidebar was last dragged to, or -1 if it never was. */
    public static int getSidebarWidth() {
        return getWidth(SIDEBAR_WIDTH);
    }

    public static boolean isSidebarHidden() {
        return Boolean.parseBoolean(properties().getProperty(SIDEBAR_HIDDEN));
    }

    public static void setSidebar(int width, boolean hidden) {
        setPanel(SIDEBAR_WIDTH, width, SIDEBAR_HIDDEN, hidden);
    }

    /** The width the selected packs panel was last dragged to, or -1 if it never was. */
    public static int getSelectedPacksWidth() {
        return getWidth(SELECTED_PACKS_WIDTH);
    }

    public static boolean isSelectedPacksHidden() {
        return Boolean.parseBoolean(properties().getProperty(SELECTED_PACKS_HIDDEN));
    }

    public static void setSelectedPacks(int width, boolean hidden) {
        setPanel(SELECTED_PACKS_WIDTH, width, SELECTED_PACKS_HIDDEN, hidden);
    }

    private static int getWidth(String key) {
        try {
            return Integer.parseInt(properties().getProperty(key, "-1"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void setPanel(String widthKey, int width, String hiddenKey, boolean hidden) {
        properties().setProperty(widthKey, String.valueOf(width));
        properties().setProperty(hiddenKey, String.valueOf(hidden));
        save();
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("vtd_ui_refresh.properties");
    }

    private static Properties properties() {
        if (properties == null) {
            properties = new Properties();

            Path file = file();
            if (Files.exists(file)) {
                try (InputStream stream = Files.newInputStream(file)) {
                    properties.load(stream);
                } catch (IOException | IllegalArgumentException e) {
                    VTDMod.LOGGER.error("Failed to read the download screen's settings", e);
                }
            }
        }

        return properties;
    }

    private static void save() {
        try (OutputStream stream = Files.newOutputStream(file())) {
            properties().store(stream, null);
        } catch (IOException e) {
            VTDMod.LOGGER.error("Failed to save the download screen's settings", e);
        }
    }
}
