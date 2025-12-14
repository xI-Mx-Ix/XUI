/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.font.layout;

import net.xmx.xui.core.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single visual line of text that has been laid out by the {@link TextLayoutEngine}.
 * Contains a list of segments (words or phrases) that share the same vertical alignment.
 *
 * @author xI-Mx-Ix
 */
public class TextLine {

    private final List<Segment> segments = new ArrayList<>();
    private float width;

    /**
     * Adds a text segment to this line.
     *
     * @param component The source component containing the style.
     * @param text      The specific text substring for this segment.
     * @param width     The pixel width of this segment.
     */
    public void add(TextComponent component, String text, float width) {
        this.segments.add(new Segment(component, text));
        this.width += width;
    }

    /**
     * @return The list of text segments in this line.
     */
    public List<Segment> getSegments() {
        return segments;
    }

    /**
     * @return The total visual width of this line in pixels.
     */
    public float getWidth() {
        return width;
    }

    /**
     * Internal record representing a piece of text within a line.
     */
    public record Segment(TextComponent component, String text) {}
}