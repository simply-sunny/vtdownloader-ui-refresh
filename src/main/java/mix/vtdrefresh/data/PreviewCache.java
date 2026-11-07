package mix.vtdrefresh.data;

import com.mojang.blaze3d.platform.NativeImage;
import me.bymartrixx.vtd.VTDMod;
import me.bymartrixx.vtd.data.Pack;
import net.minecraft.util.Util;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The preview each pack has on vanillatweaks.net, a picture or an animation, kept on disk once downloaded. The pack
 * list doesn't say which, so a missing PNG is tried as a GIF. Frames are scaled down, as some are 1920 wide.
 */
public final class PreviewCache {
    private static final Path DIRECTORY = ModFiles.cache().resolve("previews");
    private static final String[] EXTENSIONS = {"png", "gif"};
    /** Wide enough for the list at any sensible size, small enough that a long animation stays a few MB. */
    private static final int MAX_WIDTH = 400;
    private static final int MAX_FRAMES = 150;
    private static final int DEFAULT_DELAY_MS = 100;
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .executor(Util.ioPool())
            .build();

    private PreviewCache() {
    }

    /** The frames of a preview, each shown for its delay. A still picture is one frame. */
    public record Preview(List<NativeImage> frames, int[] delays, int width, int height) {
        public void close() {
            this.frames.forEach(NativeImage::close);
        }
    }

    /** Completes with null when the pack has no preview the game can show. */
    public static CompletableFuture<Preview> get(Pack pack) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                for (String extension : EXTENSIONS) {
                    Path path = getPath(pack, extension);
                    if (Files.isRegularFile(path)) {
                        return decode(Files.readAllBytes(path));
                    }
                }

                for (String extension : EXTENSIONS) {
                    byte[] bytes = download(pack, extension);
                    if (bytes != null) {
                        Path path = getPath(pack, extension);
                        try {
                            Files.createDirectories(path.getParent());
                            Files.write(path, bytes);
                        } catch (IOException e) {
                            VTDMod.LOGGER.warn("Couldn't cache the preview {}", path, e);
                        }
                        return decode(bytes);
                    }
                }
            } catch (IOException | InterruptedException e) {
                VTDMod.LOGGER.warn("Couldn't load the preview of {}", pack.getId(), e);
            }

            return null;
        }, Util.ioPool());
    }

    private static Path getPath(Pack pack, String extension) {
        String name = pack.getId().replaceAll("[^A-Za-z0-9_.-]", "_");
        return DIRECTORY.resolve(VTDMod.VT_VERSION.replaceAll("[^A-Za-z0-9_.-]", "_")).resolve(name + "." + extension);
    }

    private static byte[] download(Pack pack, String extension) throws IOException, InterruptedException {
        URI uri = URI.create(String.format("%s/assets/resources/previews/resourcepacks/%s/%s.%s",
                VTDMod.BASE_URL, VTDMod.VT_VERSION, pack.getId(), extension));
        HttpRequest request = HttpRequest.newBuilder(uri).header("User-Agent", VTDMod.USER_AGENT)
                .timeout(Duration.ofSeconds(30)).build();
        HttpResponse<byte[]> response = CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
        return response.statusCode() / 100 == 2 ? response.body() : null;
    }

    private static Preview decode(byte[] bytes) throws IOException {
        try (ImageInputStream stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) {
                return null;
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(stream, false);
                return "gif".equalsIgnoreCase(reader.getFormatName()) ? decodeGif(reader) : decodeStill(reader);
            } finally {
                reader.dispose();
            }
        }
    }

    private static Preview decodeStill(ImageReader reader) throws IOException {
        BufferedImage image = reader.read(0);
        int[] size = scaledSize(image.getWidth(), image.getHeight());
        return new Preview(List.of(toNative(scale(image, size[0], size[1]))), new int[]{0}, size[0], size[1]);
    }

    /** Draws each frame over the last, as a GIF's frames are often only the part that changed. */
    private static Preview decodeGif(ImageReader reader) throws IOException {
        int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
        int width = 0;
        int height = 0;
        IIOMetadata streamMetadata = reader.getStreamMetadata();
        if (streamMetadata != null) {
            Node screen = child(streamMetadata.getAsTree("javax_imageio_gif_stream_1.0"), "LogicalScreenDescriptor");
            if (screen != null) {
                width = intAttribute(screen, "logicalScreenWidth", 0);
                height = intAttribute(screen, "logicalScreenHeight", 0);
            }
        }
        if (width <= 0 || height <= 0) {
            BufferedImage first = reader.read(0);
            width = first.getWidth();
            height = first.getHeight();
        }

        int[] size = scaledSize(width, height);
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        List<NativeImage> frames = new ArrayList<>();
        int[] delays = new int[count];

        try {
            for (int i = 0; i < count; i++) {
                BufferedImage frame = reader.read(i);
                Node root = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
                Node descriptor = child(root, "ImageDescriptor");
                Node control = child(root, "GraphicControlExtension");
                int x = descriptor != null ? intAttribute(descriptor, "imageLeftPosition", 0) : 0;
                int y = descriptor != null ? intAttribute(descriptor, "imageTopPosition", 0) : 0;
                String disposal = control != null ? attribute(control, "disposalMethod") : "none";
                int delay = control != null ? intAttribute(control, "delayTime", 0) * 10 : 0;
                // Browsers treat a delay of almost nothing as 100 ms, and so do previews made for them
                delays[i] = delay <= 10 ? DEFAULT_DELAY_MS : delay;

                BufferedImage before = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
                g.setComposite(AlphaComposite.SrcOver);
                g.drawImage(frame, x, y, null);
                frames.add(toNative(scale(canvas, size[0], size[1])));

                if ("restoreToBackgroundColor".equals(disposal)) {
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(x, y, frame.getWidth(), frame.getHeight());
                } else if (before != null) {
                    g.setComposite(AlphaComposite.Src);
                    g.drawImage(before, 0, 0, null);
                }
            }
        } catch (IOException | RuntimeException e) {
            frames.forEach(NativeImage::close);
            throw e;
        } finally {
            g.dispose();
        }

        return new Preview(frames, delays, size[0], size[1]);
    }

    private static int[] scaledSize(int width, int height) {
        if (width <= MAX_WIDTH) {
            return new int[]{width, height};
        }
        return new int[]{MAX_WIDTH, Math.max(1, Math.round(height * (float) MAX_WIDTH / width))};
    }

    private static BufferedImage scale(BufferedImage image, int width, int height) {
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, width, height, null);
        g.dispose();
        return scaled;
    }

    private static BufferedImage copy(BufferedImage image) {
        return scale(image, image.getWidth(), image.getHeight());
    }

    private static NativeImage toNative(BufferedImage image) {
        NativeImage result = new NativeImage(image.getWidth(), image.getHeight(), false);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                result.setPixel(x, y, image.getRGB(x, y));
            }
        }
        return result;
    }

    private static Node child(Node node, String name) {
        for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (name.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    private static String attribute(Node node, String name) {
        Node attribute = node.getAttributes().getNamedItem(name);
        return attribute != null ? attribute.getNodeValue() : null;
    }

    private static int intAttribute(Node node, String name, int fallback) {
        try {
            String value = attribute(node, name);
            return value != null ? Integer.parseInt(value) : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
