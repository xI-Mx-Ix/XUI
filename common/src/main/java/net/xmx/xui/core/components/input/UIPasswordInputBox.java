/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.core.components.input;

import net.xmx.xui.core.gl.renderer.UIRenderer;
import net.xmx.xui.core.style.InteractionState;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;

/**
 * A specialized text input component for passwords.
 * <p>
 * This component functions exactly like {@link UITextInputBox}, but renders a masking character
 * (defaulting to '*') instead of the actual text content. It also enforces single-line mode
 * and disables clipboard copy/cut operations for security.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class UIPasswordInputBox extends UITextInputBox {

    /**
     * The character used to mask the password text.
     */
    private char maskCharacter = '*';

    /**
     * Constructs a default Password Input box.
     * Forces the component to be single-line.
     */
    public UIPasswordInputBox() {
        super();
        // Passwords should strictly be single-line
        super.setMultiline(false);
    }

    /**
     * Sets the character used to mask the input.
     *
     * @param maskCharacter The new mask character.
     * @return This instance for chaining.
     */
    public UIPasswordInputBox setMaskCharacter(char maskCharacter) {
        this.maskCharacter = maskCharacter;
        return this;
    }

    /**
     * Overridden to prevent enabling multi-line mode on a password field.
     *
     * @param multiline Ignored, always sets to false.
     * @return This instance.
     */
    @Override
    public UITextInputBox setMultiline(boolean multiline) {
        return super.setMultiline(false);
    }

    /**
     * Renders the password box.
     * <p>
     * This method temporarily swaps the actual text with a string of masking characters
     * of the same length. This ensures that the parent {@link UITextInputBox} renders the
     * correct visual representation (including cursor positioning based on the width
     * of the mask characters) without modifying the underlying logic.
     * </p>
     */
    @Override
    protected void drawSelf(UIRenderer renderer, int mouseX, int mouseY, float partialTicks, float deltaTime, InteractionState state) {
        // 1. Save the actual sensitive password
        String actualText = getText();

        // 2. Generate the masked string (e.g., "•••••")
        // Using repeat (Java 11+) or a simple loop builder
        String maskedText;
        if (actualText.isEmpty()) {
            maskedText = "";
        } else {
            maskedText = String.join("", Collections.nCopies(actualText.length(), String.valueOf(maskCharacter)));
        }

        // 3. Temporarily set the text to the masked version.
        // We use super.setText() to ensure internal state (like scrolling calculations in drawSelf)
        // works with the width of the dots, not the width of the real letters.
        super.setText(maskedText);

        try {
            // 4. Delegate rendering to the parent class
            super.drawSelf(renderer, mouseX, mouseY, partialTicks, deltaTime, state);
        } finally {
            // 5. Restore the actual password immediately after drawing
            // This happens before the next frame's logic updates, preserving the data.
            super.setText(actualText);
        }
    }

    /**
     * Handles keyboard input with security restrictions.
     * <p>
     * Intercepts and blocks Clipboard Copy (Ctrl+C) and Cut (Ctrl+X) operations
     * to prevent the password from being exposed to the system clipboard.
     * </p>
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused) return false;

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;

        // Security: Block Copy (Ctrl+C) and Cut (Ctrl+X)
        if (ctrl) {
            if (keyCode == GLFW.GLFW_KEY_C || keyCode == GLFW.GLFW_KEY_X) {
                // Consume the event but do nothing
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}