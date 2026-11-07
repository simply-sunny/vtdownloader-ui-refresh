package mix.vtdrefresh.gui;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the bit of HTML in Vanilla Tweaks descriptions into text: the colours of its spans, bold, italics and
 * links, with {@code <br>} as a new line and every other tag dropped.
 */
public final class RichText {
    private static final Pattern TOKEN = Pattern.compile("<(/?)([a-zA-Z0-9]+)([^>]*)>|[^<]+|<");
    private static final Pattern COLOR = Pattern.compile("color:\\s*(#[0-9a-fA-F]{6}|rgba?\\([^)]*\\))");
    private static final int LINK = 0xFF7FB8FF;

    private RichText() {
    }

    public static Component parse(String html) {
        List<Segment> segments = new ArrayList<>();
        Deque<Style> styles = new ArrayDeque<>();
        styles.push(Style.EMPTY);

        Matcher matcher = TOKEN.matcher(html);
        while (matcher.find()) {
            String tag = matcher.group(2);
            if (tag == null) {
                append(segments, styles.peek(), unescape(matcher.group()));
                continue;
            }

            boolean closing = !matcher.group(1).isEmpty();
            String name = tag.toLowerCase();
            switch (name) {
                case "br" -> newLine(segments, styles.peek());
                case "p", "div", "li" -> {
                    if (closing) newLine(segments, styles.peek());
                }
                case "span", "font", "b", "strong", "i", "em", "a", "u" -> {
                    if (closing) {
                        if (styles.size() > 1) styles.pop();
                    } else {
                        styles.push(open(styles.peek(), name, matcher.group(3)));
                    }
                }
                default -> {
                }
            }
        }

        MutableComponent text = Component.empty();
        for (Segment segment : segments) {
            String value = segment.text.toString();
            if (!value.isEmpty()) {
                text.append(Component.literal(value).withStyle(segment.style));
            }
        }
        return text;
    }

    private static Style open(Style style, String name, String attributes) {
        return switch (name) {
            case "b", "strong" -> style.withBold(true);
            case "i", "em" -> style.withItalic(true);
            case "u" -> style.withUnderlined(true);
            case "a" -> style.withColor(LINK & 0xFFFFFF);
            default -> {
                Matcher color = COLOR.matcher(attributes);
                yield color.find() ? style.withColor(Colors.parse(color.group(1)) & 0xFFFFFF) : style;
            }
        };
    }

    /** Collapses runs of spaces, drops spaces at the start of a line and before punctuation, as a browser would. */
    private static void append(List<Segment> segments, Style style, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean space = Character.isWhitespace(c) || c == ' ';
            char last = lastChar(segments);
            if (space) {
                if (last != 0 && last != ' ' && last != '\n') {
                    segment(segments, style).text.append(' ');
                }
                continue;
            }
            if (last == ' ' && ",.!?;:)".indexOf(c) != -1) {
                dropTrailingSpace(segments);
            }
            segment(segments, style).text.append(c);
        }
    }

    private static void newLine(List<Segment> segments, Style style) {
        dropTrailingSpace(segments);
        if (lastChar(segments) != 0) {
            segment(segments, style).text.append('\n');
        }
    }

    private static Segment segment(List<Segment> segments, Style style) {
        if (segments.isEmpty() || !segments.getLast().style.equals(style)) {
            segments.add(new Segment(style, new StringBuilder()));
        }
        return segments.getLast();
    }

    private static char lastChar(List<Segment> segments) {
        for (int i = segments.size() - 1; i >= 0; i--) {
            StringBuilder text = segments.get(i).text;
            if (!text.isEmpty()) {
                return text.charAt(text.length() - 1);
            }
        }
        return 0;
    }

    private static void dropTrailingSpace(List<Segment> segments) {
        for (int i = segments.size() - 1; i >= 0; i--) {
            StringBuilder text = segments.get(i).text;
            if (!text.isEmpty()) {
                if (text.charAt(text.length() - 1) == ' ') {
                    text.setLength(text.length() - 1);
                }
                return;
            }
        }
    }

    private static String unescape(String text) {
        return text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'").replace("&amp;", "&");
    }

    private record Segment(Style style, StringBuilder text) {
    }
}
