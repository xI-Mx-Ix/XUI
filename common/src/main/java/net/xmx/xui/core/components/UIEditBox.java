/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components;

import net.xmx.xui.core.font.DefaultFonts;
import net.xmx.xui.core.font.Font;
import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.CornerRadii;
import net.xmx.xui.core.style.InteractionState;
import net.xmx.xui.core.style.StyleKey;
import net.xmx.xui.core.style.ThemeProperties;
import net.xmx.xui.core.text.TextComponent;
import net.xmx.xui.core.UIWidget;
import net.xmx.xui.core.effect.UIScissorsEffect;
import net.xmx.xui.core.util.ClipboardUtil;
import org.lwjgl.glfw.GLFW;

/**
 * A robust text input component (EditBox).
 *
 * <ul>
 *   <li>Single-line and Multi-line support.</li>
 *   <li>Text Selection via Mouse and Keyboard (Shift+Arrows).</li>
 *   <li>Clipboard operations (Ctrl+C, V, X, A).</li>
 *   <li>Smoothly fading cursor animation.</li>
 *   <li>Auto-scrolling.</li>
 * </ul>
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIEditBox extends UIWidget {

    /**
     * Property for the color of the text cursor.
     */
    public static final StyleKey<Integer> CURSOR_COLOR = new StyleKey<>("cursor_color", 0xFFE0E0E0);

    /**
     * Property for the background color of the text selection.
     */
    public static final StyleKey<Integer> SELECTION_COLOR = new StyleKey<>("selection_color", 0x800000FF);

    /**
     * Property for the color of the hint text (placeholder).
     */
    public static final StyleKey<Integer> HINT_COLOR = new StyleKey<>("hint_color", 0xFF888888);

    /**
     * The font instance used to render text within this EditBox.
     * Defaults to the standard Vanilla font.
     */
    private Font font = DefaultFonts.getVanilla();

    private String hintText = "";
    private String text = "";
    private int cursorPosition = 0;
    private int selectionEnd = 0;
    private int maxLength = 1024;
    private boolean isMultiline = false;

    // Scrolling state
    private float scrollX = 0.0f;
    private float scrollY = 0.0f;
    private float maxScrollX = 0.0f;
    private float maxScrollY = 0.0f;

    // Layout configuration
    private final float padding = 4.0f;

    /**
     * Constructs a default EditBox.
     */
    public UIEditBox() {
        setupStyles();
        this.addEffect(new UIScissorsEffect());
    }

    /**
     * Configures the default style properties.
     */
    private void setupStyles() {
        this.style()
                .set(InteractionState.DEFAULT, ThemeProperties.BACKGROUND_COLOR, 0xFF101010)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_COLOR, 0xFF606060)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_THICKNESS, 1.0f)
                .set(InteractionState.DEFAULT, ThemeProperties.BORDER_RADIUS, CornerRadii.all(3.0f))
                .set(InteractionState.DEFAULT, ThemeProperties.TEXT_COLOR, 0xFFFFFFFF)
                .set(InteractionState.DEFAULT, HINT_COLOR, 0xFF888888) // Default gray for hint
                .set(InteractionState.DEFAULT, CURSOR_COLOR, 0xFFFFFFFF)
                .set(InteractionState.DEFAULT, SELECTION_COLOR, 0x800000AA)
                .set(InteractionState.HOVER, ThemeProperties.BORDER_COLOR, 0xFFAAAAAA)
                .set(InteractionState.ACTIVE, ThemeProperties.BORDER_COLOR, 0xFFFFFFFF);
    }


    /**
     * Toggles multi-line mode.
     *
     * @param multiline True to allow multiple lines.
     * @return This instance.
     */
    public UIEditBox setMultiline(boolean multiline) {
        this.isMultiline = multiline;
        return this;
    }

    /**
     * Sets the hint text (placeholder) to display when the box is empty and unfocused.
     *
     * @param hint The hint text.
     * @return This instance.
     */
    public UIEditBox setHint(String hint) {
        this.hintText = hint;
        return this;
    }

    /**
     * Gets the current text.
     *
     * @return The text.
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the font used by this text box.
     *
     * @param font The font instance.
     * @return This widget instance for chaining.
     */
    public UIEditBox setFont(Font font) {
        this.font = font;
        return this;
    }

    /**
     * Gets the currently active font.
     *
     * @return The font instance.
     */
    public Font getFont() {
        return font;
    }

    /**
     * Sets the current text.
     *
     * @param text The new text.
     */
    public void setText(String text) {
        this.text = text;
        this.cursorPosition = Math.min(cursorPosition, text.length());
        this.selectionEnd = Math.min(selectionEnd, text.length());
    }

    /**
     * Sets the maximum character length.
     *
     * @param maxLength Max characters.
     */
    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
        if (text.length() > maxLength) {
            text = text.substring(0, maxLength);
        }
    }

    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // Retrieve style properties and animated colors
        int bgColor = getColor(ThemeProperties.BACKGROUND_COLOR, state, deltaTime);
        int borderColor = getColor(ThemeProperties.BORDER_COLOR, state, deltaTime);
        int textColor = getColor(ThemeProperties.TEXT_COLOR, state, deltaTime);
        int hintColor = getColor(HINT_COLOR, state, deltaTime);
        int cursorColor = getColor(CURSOR_COLOR, state, deltaTime);
        int selectionColor = getColor(SELECTION_COLOR, state, deltaTime);

        CornerRadii radii = getCornerRadii(ThemeProperties.BORDER_RADIUS, state, deltaTime);
        float borderThick = getFloat(ThemeProperties.BORDER_THICKNESS, state, deltaTime);

        // Render the background and border of the edit box
        renderer.getGeometry().renderRect(x, y, width, height, bgColor,
                radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft());
        if (borderThick > 0) {
            renderer.getGeometry().renderOutline(x, y, width, height, borderColor, borderThick,
                    radii.topLeft(), radii.topRight(), radii.bottomRight(), radii.bottomLeft());
        }

        // Use the instance font height instead of static global height
        int fontHeight = (int) font.getLineHeight();
        float contentX = x + padding;

        // Recalculate scroll offsets based on text content and dimensions
        updateScrolling();

        // Calculate the base coordinates for drawing text
        float drawX = contentX - scrollX;
        float drawY;

        if (isMultiline) {
            // In multi-line mode, text starts at the top padding and scrolls vertically via scrollY
            drawY = (y + padding) - scrollY;
        } else {
            // In single-line mode, text is vertically centered; scrollY is ignored
            drawY = y + (height - fontHeight) / 2.0f;
        }

        // Render the background highlight for selected text
        if (cursorPosition != selectionEnd) {
            renderSelection(renderer, drawX, drawY, selectionColor);
        }

        // If the box is empty, not focused, and has a hint, draw the hint.
        if (text.isEmpty() && !isFocused && !hintText.isEmpty()) {
            if (isMultiline) {
                String[] lines = hintText.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    // Create component with the specific font
                    renderer.drawText(TextComponent.literal(lines[i]).setFont(font), drawX, drawY + (i * fontHeight), hintColor, true);
                }
            } else {
                renderer.drawText(TextComponent.literal(hintText).setFont(font), drawX, drawY, hintColor, true);
            }
        } else {
            // Otherwise, render the actual text content
            if (isMultiline) {
                String[] lines = text.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    renderer.drawText(TextComponent.literal(lines[i]).setFont(font), drawX, drawY + (i * fontHeight), textColor, true);
                }
            } else {
                renderer.drawText(TextComponent.literal(text).setFont(font), drawX, drawY, textColor, true);
            }
        }

        // Render the blinking cursor if the widget has focus
        if (isFocused) {
            renderCursor(renderer, drawX, drawY, fontHeight, cursorColor);
        }
    }

    private void renderCursor(UIRenderer renderer, float baseX, float baseY, int fontHeight, int color) {
        // Smooth blink logic
        double time = System.currentTimeMillis() / 250.0;
        float alphaFactor = (float) (0.5 + 0.5 * Math.sin(time));
        int baseAlpha = (color >> 24) & 0xFF;
        int finalAlpha = (int) (baseAlpha * alphaFactor);
        int blinkingColor = (color & 0x00FFFFFF) | (finalAlpha << 24);

        float cx, cy;
        if (isMultiline) {
            int pos = 0;
            int lineIndex = 0;
            String[] lines = text.split("\n", -1);
            int colIndex = 0;
            boolean found = false;

            for (String line : lines) {
                if (cursorPosition <= pos + line.length()) {
                    colIndex = cursorPosition - pos;
                    found = true;
                    break;
                }
                pos += line.length() + 1;
                lineIndex++;
            }
            if (!found) {
                lineIndex = lines.length - 1;
                colIndex = lines[lineIndex].length();
            }

            String subLine = lines[lineIndex].substring(0, colIndex);
            // Calculate width using the instance font
            cx = font.getWidth(TextComponent.literal(subLine).setFont(font));
            cy = lineIndex * fontHeight;
        } else {
            String sub = text.substring(0, cursorPosition);
            cx = font.getWidth(TextComponent.literal(sub).setFont(font));
            cy = 0;
        }

        float cursorX = baseX + cx;
        float cursorY = baseY + cy;

        // Draw the cursor line
        renderer.getGeometry().renderRect(cursorX, cursorY - 1, 1, fontHeight + 2, blinkingColor, 0);
    }

    private void renderSelection(UIRenderer renderer, float baseX, float baseY, int color) {
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        int fontHeight = (int) font.getLineHeight();

        if (isMultiline) {
            int pos = 0;
            String[] lines = text.split("\n", -1);

            for (int i = 0; i < lines.length; i++) {
                int lineLen = lines[i].length();
                int lineStart = pos;
                int lineEnd = pos + lineLen;

                if (start <= lineEnd && end >= lineStart) {
                    int s = Math.max(start, lineStart) - lineStart;
                    int e = Math.min(end, lineEnd) - lineStart;

                    if (s < e || (s == e && lineStart >= start && lineEnd <= end)) {
                        // Measure segments using the instance font
                        float x1 = font.getWidth(TextComponent.literal(lines[i].substring(0, s)).setFont(font));
                        float x2 = font.getWidth(TextComponent.literal(lines[i].substring(0, e)).setFont(font));

                        if (end > lineEnd) x2 += 4; // Visual padding for newline selection

                        renderer.getGeometry().renderRect(baseX + x1, baseY + (i * fontHeight), x2 - x1, fontHeight, color, 0);
                    }
                }
                pos += lineLen + 1;
            }
        } else {
            float x1 = font.getWidth(TextComponent.literal(text.substring(0, start)).setFont(font));
            float x2 = font.getWidth(TextComponent.literal(text.substring(0, end)).setFont(font));
            renderer.getGeometry().renderRect(baseX + x1, baseY, x2 - x1, fontHeight, color, 0);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused) return false;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE:
                if (!text.isEmpty()) {
                    if (cursorPosition != selectionEnd) {
                        deleteSelection();
                    } else if (cursorPosition > 0) {
                        StringBuilder sb = new StringBuilder(text);
                        sb.deleteCharAt(cursorPosition - 1);
                        text = sb.toString();
                        moveCursor(-1, false);
                    }
                }
                return true;

            case GLFW.GLFW_KEY_DELETE:
                if (cursorPosition != selectionEnd) {
                    deleteSelection();
                } else if (cursorPosition < text.length()) {
                    StringBuilder sb = new StringBuilder(text);
                    sb.deleteCharAt(cursorPosition);
                    text = sb.toString();
                }
                return true;

            case GLFW.GLFW_KEY_LEFT:
                moveCursor(-1, shift);
                return true;

            case GLFW.GLFW_KEY_RIGHT:
                moveCursor(1, shift);
                return true;

            case GLFW.GLFW_KEY_UP:
                moveVertical(-1, shift);
                return true;

            case GLFW.GLFW_KEY_DOWN:
                moveVertical(1, shift);
                return true;

            case GLFW.GLFW_KEY_HOME:
                setCursorPos(0, shift);
                return true;

            case GLFW.GLFW_KEY_END:
                setCursorPos(text.length(), shift);
                return true;

            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                if (isMultiline) {
                    insertText("\n");
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_A:
                if (ctrl) {
                    cursorPosition = text.length();
                    selectionEnd = 0;
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_C:
                if (ctrl) {
                    copyToClipboard();
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_V:
                if (ctrl) {
                    pasteFromClipboard();
                    return true;
                }
                break;

            case GLFW.GLFW_KEY_X:
                if (ctrl) {
                    copyToClipboard();
                    deleteSelection();
                    return true;
                }
                break;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused) return false;
        if (isValidChar(codePoint)) {
            insertText(Character.toString(codePoint));
            return true;
        }
        return false;
    }

    /**
     * Checks if a character is valid for input (Printable ASCII/Unicode).
     * Replaces SharedConstants.isAllowedChatCharacter.
     */
    private boolean isValidChar(char c) {
        return c >= 32 && c != 127;
    }

    private void insertText(String str) {
        if (text.length() + str.length() > maxLength) return;
        if (cursorPosition != selectionEnd) deleteSelection();

        StringBuilder sb = new StringBuilder(text);
        sb.insert(cursorPosition, str);
        text = sb.toString();
        moveCursor(str.length(), false);
    }

    private void deleteSelection() {
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);
        if (start == end) return;
        StringBuilder sb = new StringBuilder(text);
        sb.delete(start, end);
        text = sb.toString();
        setCursorPos(start, false);
    }

    private void moveCursor(int delta, boolean keepSelection) {
        setCursorPos(cursorPosition + delta, keepSelection);
    }

    private void setCursorPos(int pos, boolean keepSelection) {
        this.cursorPosition = Math.max(0, Math.min(text.length(), pos));
        if (!keepSelection) this.selectionEnd = this.cursorPosition;
    }

    /**
     * Moves the cursor vertically to the adjacent line while attempting to maintain
     * the visual horizontal position (X-coordinate).
     *
     * @param lineOffset    The direction and distance to move (e.g., -1 for up, 1 for down).
     * @param keepSelection Whether to expand the selection (Shift held) or reset it.
     */
    private void moveVertical(int lineOffset, boolean keepSelection) {
        if (!isMultiline) {
            // In single-line mode, behave like Home (Up) or End (Down)
            if (lineOffset < 0) {
                setCursorPos(0, keepSelection);
            } else {
                setCursorPos(text.length(), keepSelection);
            }
            return;
        }

        String[] lines = text.split("\n", -1);

        // Find the index of the line containing the cursor
        int currentLineIndex = 0;
        int currentLineGlobalStart = 0;
        int accumulatedLength = 0;

        for (int i = 0; i < lines.length; i++) {
            int lineLen = lines[i].length();
            if (cursorPosition <= accumulatedLength + lineLen) {
                currentLineIndex = i;
                currentLineGlobalStart = accumulatedLength;
                break;
            }
            accumulatedLength += lineLen + 1; // +1 for the newline character
        }

        // Calculate target line index
        int targetLineIndex = currentLineIndex + lineOffset;

        // Ensure target is within bounds
        if (targetLineIndex < 0 || targetLineIndex >= lines.length) {
            return;
        }

        // Calculate the visual X offset in the current line
        int localIndexInCurrentLine = cursorPosition - currentLineGlobalStart;
        String currentLineText = lines[currentLineIndex];

        // Clamp index to valid range for the current line
        localIndexInCurrentLine = Math.max(0, Math.min(localIndexInCurrentLine, currentLineText.length()));

        String subStr = currentLineText.substring(0, localIndexInCurrentLine);
        float currentVisX = font.getWidth(TextComponent.literal(subStr).setFont(font));

        // Find the index in the target line closest to the calculated X offset
        String targetLineText = lines[targetLineIndex];
        int localIndexInTargetLine = getIndexInLine(targetLineText, currentVisX);

        // Calculate global position for the start of the target line
        int targetLineGlobalStart = 0;
        for (int i = 0; i < targetLineIndex; i++) {
            targetLineGlobalStart += lines[i].length() + 1;
        }

        setCursorPos(targetLineGlobalStart + localIndexInTargetLine, keepSelection);
    }

    private void copyToClipboard() {
        int start = Math.min(cursorPosition, selectionEnd);
        int end = Math.max(cursorPosition, selectionEnd);

        if (start < end) {
            String selected = text.substring(start, end);
            long window = GLFW.glfwGetCurrentContext();

            // Delegate to the Singleton ClipboardUtil
            ClipboardUtil.getInstance().setClipboardString(window, selected);
        }
    }

    private void pasteFromClipboard() {
        long window = GLFW.glfwGetCurrentContext();

        // Delegate to the Singleton ClipboardUtil
        String content = ClipboardUtil.getInstance().getClipboardString(window);

        if (content != null && !content.isEmpty()) {
            StringBuilder filtered = new StringBuilder();
            for (char c : content.toCharArray()) {
                if (isValidChar(c) || (isMultiline && c == '\n')) {
                    filtered.append(c);
                }
            }
            insertText(filtered.toString());
        }
    }

    /**
     * Handles mouse clicks to position the text cursor.
     * Requires the Left Mouse Button (ID 0) to set the caret position.
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     * @param button The button index (0 = Left Click).
     * @return {@code true} if the event was handled.
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        // If the widget is focused and the user Left Clicks inside, update the cursor position.
        // Also checks for Shift key to handle text selection initialization.
        if (isFocused && button == 0) {
            boolean shift = (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS) ||
                    (GLFW.glfwGetKey(GLFW.glfwGetCurrentContext(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS);
            int index = getIndexAtPosition(mouseX, mouseY);
            setCursorPos(index, shift);
            return true;
        }
        return handled;
    }

    /**
     * Handles text selection via mouse dragging.
     * Active only when the Left Mouse Button (ID 0) is held down.
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     * @param button The button currently held down (0 = Left Click).
     * @param dragX  The horizontal drag delta.
     * @param dragY  The vertical drag delta.
     * @return {@code true} if the event was handled.
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Only allow text selection dragging with the primary mouse button.
        if (isFocused && button == 0) {
            int index = getIndexAtPosition(mouseX, mouseY);
            // Update cursor position while maintaining the selection anchor
            setCursorPos(index, true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * Handles the mouse release event specific to the text box.
     * <p>
     * Unlike buttons, an edit box must retain its focus (and thus its {@code ACTIVE} state)
     * after the mouse button is released to allow the user to continue typing.
     * We unconditionally consume the event if the widget is currently focused, preventing
     * any parent logic from resetting the focus.
     * </p>
     *
     * @param mouseX The absolute X coordinate.
     * @param mouseY The absolute Y coordinate.
     * @param button The button released.
     * @return true if the event was handled (consumed).
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // If the box is currently focused (typing mode), we consume the release event.
        // This stops the event from bubbling up and potentially causing the screen
        // or parent widget to think the interaction is "finished" and clear the focus.
        if (this.isFocused) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * Calculates the text index at the given mouse coordinates.
     * Uses scrollX and scrollY to translate screen coordinates to text coordinates.
     */
    private int getIndexAtPosition(double mouseX, double mouseY) {
        float relativeX = (float) mouseX - (x + padding) + scrollX;
        float relativeY = (float) mouseY - (y + padding) + (isMultiline ? scrollY : 0);
        float fontHeight = font.getLineHeight();

        if (isMultiline) {
            String[] lines = text.split("\n", -1);
            int lineIdx = (int) (relativeY / fontHeight);
            if (lineIdx < 0) lineIdx = 0;
            if (lineIdx >= lines.length) lineIdx = lines.length - 1;

            int absIndex = 0;
            for (int i = 0; i < lineIdx; i++) absIndex += lines[i].length() + 1;

            return absIndex + getIndexInLine(lines[lineIdx], relativeX);
        } else {
            return getIndexInLine(text, relativeX);
        }
    }

    /**
     * Helper to find the index within a single line string for a specific visual X coordinate.
     * Iteratively measures substrings using the specific {@link #font} instance to account
     * for kerning and variable widths of custom fonts.
     */
    private int getIndexInLine(String line, float targetX) {
        if (line.isEmpty()) return 0;

        // Iterate through the string to find the closest character split
        for (int i = 0; i < line.length(); i++) {
            String sub = line.substring(0, i + 1);
            float width = font.getWidth(TextComponent.literal(sub).setFont(font));

            if (width > targetX) {
                // Determine if we are closer to the previous character or this one
                String prevSub = line.substring(0, i);
                float prevWidth = font.getWidth(TextComponent.literal(prevSub).setFont(font));

                // If target is closer to prevWidth than width, return i (before current char)
                // Otherwise return i + 1 (after current char)
                if (targetX - prevWidth < width - targetX) {
                    return i;
                } else {
                    return i + 1;
                }
            }
        }
        return line.length();
    }

    private void updateScrolling() {
        float visibleWidth = width - (padding * 2);
        float visibleHeight = height - (padding * 2);
        int fontHeight = (int) font.getLineHeight();

        if (isMultiline) {
            String[] lines = text.split("\n", -1);
            int totalLines = lines.length;
            int totalHeight = totalLines * fontHeight;
            maxScrollY = Math.max(0, totalHeight - visibleHeight);

            // Identify current line and position within that line
            int currentLineIndex = 0;
            int pos = 0;
            int lineStartPos = 0;

            for (String line : lines) {
                if (cursorPosition <= pos + line.length()) {
                    lineStartPos = pos;
                    break;
                }
                pos += line.length() + 1;
                currentLineIndex++;
            }
            if (currentLineIndex >= lines.length) {
                currentLineIndex = lines.length - 1;
                lineStartPos = pos - (lines[currentLineIndex].length() + 1);
            }

            // --- Vertical Scroll Update (Y) ---
            float cursorY = currentLineIndex * fontHeight;

            if (cursorY < scrollY) {
                scrollY = cursorY;
            } else if (cursorY + fontHeight > scrollY + visibleHeight) {
                scrollY = cursorY + fontHeight - visibleHeight;
            }
            scrollY = Math.max(0, Math.min(scrollY, maxScrollY));

            // --- Horizontal Scroll Update (X) ---
            String currentLineText = lines[currentLineIndex];
            int localCursorIndex = Math.max(0, Math.min(currentLineText.length(), cursorPosition - lineStartPos));
            String subLine = currentLineText.substring(0, localCursorIndex);

            int cursorX = (int) font.getWidth(TextComponent.literal(subLine).setFont(font));

            if (cursorX < scrollX) {
                scrollX = cursorX;
            } else if (cursorX > scrollX + visibleWidth - 4) {
                scrollX = cursorX - visibleWidth + 4;
            }

            // Calculate max X scroll for the current line to prevent empty space
            int lineWidth = (int) font.getWidth(TextComponent.literal(currentLineText).setFont(font));
            maxScrollX = Math.max(0, lineWidth - visibleWidth + 8);
            scrollX = Math.max(0, Math.min(scrollX, maxScrollX));

        } else {
            // Single-line logic: Only X scrolling is relevant
            String sub = text.substring(0, cursorPosition);
            int cursorX = (int) font.getWidth(TextComponent.literal(sub).setFont(font));

            if (cursorX < scrollX) {
                scrollX = cursorX;
            } else if (cursorX > scrollX + visibleWidth - 4) {
                scrollX = cursorX - visibleWidth + 4;
            }

            int textWidth = (int) font.getWidth(TextComponent.literal(text).setFont(font));
            maxScrollX = Math.max(0, textWidth - visibleWidth + 8);
            scrollX = Math.max(0, Math.min(scrollX, maxScrollX));

            // Reset Y scroll in single-line mode
            scrollY = 0;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} to ensure the edit box retains focus for keyboard input
     * after the user releases the mouse button.
     * </p>
     */
    @Override
    protected boolean shouldRetainFocus() {
        return true;
    }
}