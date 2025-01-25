package net.microfalx.maven.report;

import net.microfalx.lang.StringUtils;
import net.microfalx.resource.Resource;

import java.awt.*;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static net.microfalx.lang.ArgumentUtils.requireNonNull;

/**
 * Converts a text with ANSI escape characters to HTML
 */
public class AnsiToHtml {

    private static final char FIRST_ESC_CHAR = 27;
    private static final char SECOND_ESC_CHAR = '[';

    private static final char CSS_STYLE_SEPARATOR = ';';

    private final StringBuilder builder = new StringBuilder();
    private final StringBuilder lineBuilder = new StringBuilder();

    private boolean escaped = false;
    private char[] buffer;
    private int bufferIndex;

    private final Set<GraphicsMode> graphicsModes = EnumSet.noneOf(GraphicsMode.class);
    private Color foregroundColor;
    private Color backgroundColor;

    /**
     * Parses and Transforms the ANSI text.
     *
     * @param resource the content to parse
     * @return the HTML output
     * @throws IOException if an I/O error occurs
     */
    public Resource transform(Resource resource) throws IOException {
        requireNonNull(resource);
        LineNumberReader lineReader = new LineNumberReader(resource.getReader());
        String line;
        while ((line = lineReader.readLine()) != null) {
            line = transform(line);
            builder.append(line).append("\n");
        }
        return Resource.text(builder.toString());
    }

    private String transform(String line) {
        lineBuilder.setLength(0);
        bufferIndex = 0;
        buffer = line.toCharArray();
        while (bufferIndex < buffer.length) {
            char c = buffer[bufferIndex++];
            if (c == FIRST_ESC_CHAR) {
                escaped = true;
                c = buffer[bufferIndex++];
                if (c == SECOND_ESC_CHAR) {
                    while (escaped) {
                        collectEscapes();
                    }
                    if (hasStyles()) {
                        lineBuilder.append("<span style='").append(getHtmlStyle()).append("'>");
                    } else {
                        lineBuilder.append("</span>");
                    }
                }
            } else {
                lineBuilder.append(c);
            }
        }
        return lineBuilder.toString();
    }

    private String getHtmlStyle() {
        StringBuilder styleBuilder = new StringBuilder();
        addGraphicsModeToCss(styleBuilder);
        addColor(styleBuilder, "color", foregroundColor);
        addColor(styleBuilder, "background-color", backgroundColor);
        resetStyles();
        return styleBuilder.toString();
    }

    private void addGraphicsModeToCss(StringBuilder styleBuilder) {
        if (graphicsModes.contains(GraphicsMode.BOLD)) {
            StringUtils.append(styleBuilder, "font-weight: bold", CSS_STYLE_SEPARATOR);
        }
        if (graphicsModes.contains(GraphicsMode.ITALIC)) {
            StringUtils.append(styleBuilder, "font-style: italic", CSS_STYLE_SEPARATOR);
        }
        if (graphicsModes.contains(GraphicsMode.UNDERLINE)) {
            StringUtils.append(styleBuilder, "text-decoration: underline", CSS_STYLE_SEPARATOR);
        }
    }

    private void addColor(StringBuilder styleBuilder, String name, Color color) {
        if (color == null) return;
        StringUtils.append(styleBuilder, name + ": " + colorToCss(color), CSS_STYLE_SEPARATOR);
    }

    private String colorToCss(Color color) {
        color = color.darker();
        return "#" + toHex(color.getRed()) + toHex(color.getGreen()) + toHex(color.getBlue());
    }

    private String toHex(int value) {
        return org.apache.commons.lang3.StringUtils.leftPad(Integer.toHexString(value), 2, '0');
    }

    private char peek() {
        return buffer[bufferIndex];
    }

    private void resetStyles() {
        foregroundColor = null;
        backgroundColor = null;
        graphicsModes.clear();
    }

    private boolean hasStyles() {
        return foregroundColor != null || backgroundColor != null || !graphicsModes.isEmpty();
    }

    private void collectEscapes() {
        String nextToken = getNextToken();
        Integer mode = toInt(nextToken);
        if (mode != null) {
            GraphicsMode graphicsMode = SET_GRAPHICS_MODES.get(mode);
            if (graphicsMode != null) {
                graphicsModes.add(graphicsMode);
            } else {
                graphicsMode = RESET_GRAPHICS_MODES.get(mode);
                if (graphicsMode != null) {
                    graphicsModes.remove(graphicsMode);
                } else if (mode >= 30 && mode <= 39) {
                    foregroundColor = getColor(mode - 30);
                } else if (mode >= 40 && mode <= 49) {
                    backgroundColor = getColor(mode - 40);
                }
            }
        }
    }

    private Color getColor(int value) {
        Color color = HTML_COLORS.get(value);
        if (color == null) color = Color.BLACK;
        return color;
    }

    private String getNextToken() {
        StringBuilder tokenBuilder = new StringBuilder();
        while (bufferIndex < buffer.length) {
            char c = buffer[bufferIndex++];
            if (c == ';' || c == 'm') {
                if (c == 'm') escaped = false;
                break;
            } else {
                tokenBuilder.append(c);
            }
        }
        return tokenBuilder.toString();
    }

    private Integer toInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    enum GraphicsMode {
        RESET,
        BOLD,
        FAINT,
        ITALIC,
        UNDERLINE,
        BLINKING,
        INVERSE,
        HIDDEN,
        STRIKE_THROUGH
    }

    private static final Map<Integer, GraphicsMode> SET_GRAPHICS_MODES = new HashMap<>();
    private static final Map<Integer, GraphicsMode> RESET_GRAPHICS_MODES = new HashMap<>();
    private static final Map<Integer, Color> HTML_COLORS = new HashMap<>();
    private static final Color DEFAULT = new Color(0, 0, 0);

    static {
        SET_GRAPHICS_MODES.put(0, GraphicsMode.RESET);
        SET_GRAPHICS_MODES.put(1, GraphicsMode.BOLD);
        SET_GRAPHICS_MODES.put(2, GraphicsMode.FAINT);
        SET_GRAPHICS_MODES.put(3, GraphicsMode.ITALIC);
        SET_GRAPHICS_MODES.put(4, GraphicsMode.UNDERLINE);
        SET_GRAPHICS_MODES.put(5, GraphicsMode.BLINKING);
        SET_GRAPHICS_MODES.put(7, GraphicsMode.INVERSE);
        SET_GRAPHICS_MODES.put(8, GraphicsMode.HIDDEN);
        SET_GRAPHICS_MODES.put(9, GraphicsMode.STRIKE_THROUGH);

        RESET_GRAPHICS_MODES.put(22, GraphicsMode.BOLD);
        //RESET_GRAPHICS_MODES.put(22, GraphicsMode.FAINT);
        RESET_GRAPHICS_MODES.put(23, GraphicsMode.ITALIC);
        RESET_GRAPHICS_MODES.put(24, GraphicsMode.UNDERLINE);
        RESET_GRAPHICS_MODES.put(25, GraphicsMode.BLINKING);
        RESET_GRAPHICS_MODES.put(27, GraphicsMode.INVERSE);
        RESET_GRAPHICS_MODES.put(28, GraphicsMode.HIDDEN);
        RESET_GRAPHICS_MODES.put(29, GraphicsMode.STRIKE_THROUGH);

        HTML_COLORS.put(0, Color.BLACK);
        HTML_COLORS.put(1, Color.RED);
        HTML_COLORS.put(2, Color.GREEN);
        HTML_COLORS.put(3, Color.YELLOW);
        HTML_COLORS.put(4, Color.BLUE);
        HTML_COLORS.put(5, Color.MAGENTA);
        HTML_COLORS.put(7, Color.CYAN);
        HTML_COLORS.put(8, Color.WHITE);
        HTML_COLORS.put(9, DEFAULT);
    }

}
