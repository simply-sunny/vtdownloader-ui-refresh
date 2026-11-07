package mix.vtdrefresh.gui;

import me.bymartrixx.vtd.VTDMod;
import net.minecraft.util.ARGB;

import java.util.Arrays;
import java.util.List;

public final class Colors {
    private Colors() {
    }

    /**
     * Parse an 0xAARRGGBB color from `rgba(red, green, blue, alpha)` or `#RRGGBB`.
     * VTDownloader's own parser throws on the hex form, which Vanilla Tweaks uses for some warnings.
     */
    public static int parse(String color) {
        try {
            if (color.startsWith("#") && color.length() == 7) {
                return 0xFF000000 | Integer.parseInt(color.substring(1), 16);
            }

            if (color.startsWith("rgba(") && color.endsWith(")")) {
                List<String> components = Arrays.stream(color.substring(color.indexOf("(") + 1, color.length() - 1)
                        .split(",")).map(String::trim).toList();

                if (components.size() == 4) {
                    int red = Integer.parseInt(components.get(0));
                    int green = Integer.parseInt(components.get(1));
                    int blue = Integer.parseInt(components.get(2));
                    float alpha = Float.parseFloat(components.get(3));

                    return ARGB.color((int) (alpha * 255), red, green, blue);
                }
            }
        } catch (NumberFormatException ignored) {
        }

        VTDMod.LOGGER.warn("Unknown color format: {}", color);
        return 0x00000000;
    }
}
