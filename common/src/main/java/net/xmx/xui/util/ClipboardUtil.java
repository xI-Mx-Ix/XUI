/*
 * This file is part of XUI.
 * Licensed under LGPL 3.0.
 */
package net.xmx.xui.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Manages system clipboard interactions using LWJGL/GLFW.
 * <p>
 * This class implements a Singleton pattern to provide a centralized point
 * for clipboard operations. It handles memory management manually to reduce
 * garbage collection overhead and provides error handling for non-text clipboard formats.
 * </p>
 *
 * @author xI-Mx-Ix
 */
public class ClipboardUtil {

    private static final ClipboardUtil INSTANCE = new ClipboardUtil();

    /**
     * Default size for the scratch buffer (8 KB).
     * This size is sufficient for most standard text copy-paste operations.
     */
    private static final int BUFFER_SIZE = 8192;

    /**
     * GLFW error code indicating that the requested format is not available
     * (e.g., trying to read text when the clipboard contains an image).
     */
    private static final int GLFW_FORMAT_UNAVAILABLE = 0x10009;

    /**
     * A pre-allocated reusable buffer used to avoid repeated native memory allocations
     * for small strings.
     */
    private final ByteBuffer reusableBuffer;

    /**
     * Private constructor to enforce Singleton pattern.
     * Initializes the reusable scratch buffer.
     */
    private ClipboardUtil() {
        this.reusableBuffer = BufferUtils.createByteBuffer(BUFFER_SIZE);
    }

    /**
     * Returns the singleton instance of the ClipboardUtil.
     *
     * @return The singleton instance.
     */
    public static ClipboardUtil getInstance() {
        return INSTANCE;
    }

    /**
     * Sets the system clipboard content to the specified string.
     * <p>
     * This method converts the string to UTF-8 bytes. If the string fits into the
     * pre-allocated 8KB buffer, it is used directly. Otherwise, a temporary
     * native memory block is allocated and freed immediately after use.
     * </p>
     *
     * @param windowHandle The native handle of the GLFW window.
     * @param text         The text to copy to the clipboard. If null, the operation is ignored.
     */
    public void setClipboardString(long windowHandle, String text) {
        if (text == null) {
            return;
        }

        byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
        // C-strings require a null terminator (\0), so we need length + 1.
        int requiredSize = textBytes.length + 1;

        if (requiredSize <= reusableBuffer.capacity()) {
            // Optimization: Use the existing reusable buffer
            uploadToClipboard(windowHandle, reusableBuffer, textBytes);
        } else {
            // Fallback: Allocate specific native memory for large strings
            ByteBuffer largeBuffer = MemoryUtil.memAlloc(requiredSize);
            try {
                uploadToClipboard(windowHandle, largeBuffer, textBytes);
            } finally {
                // Critical: Explicitly free the manually allocated native memory
                MemoryUtil.memFree(largeBuffer);
            }
        }
    }

    /**
     * Helper method to populate a ByteBuffer and send it to GLFW.
     *
     * @param windowHandle The native window handle.
     * @param buffer       The target buffer (must be large enough).
     * @param bytes        The raw UTF-8 bytes of the string.
     */
    private void uploadToClipboard(long windowHandle, ByteBuffer buffer, byte[] bytes) {
        buffer.clear();
        buffer.put(bytes);
        buffer.put((byte) 0); // Null terminator
        buffer.flip();        // Prepare buffer for reading by GLFW
        GLFW.glfwSetClipboardString(windowHandle, buffer);
    }

    /**
     * Retrieves the current content of the system clipboard.
     * <p>
     * This method temporarily suppresses specific GLFW errors (like unavailable formats)
     * to prevent console spam if the clipboard contains non-text data.
     * It also sanitizes the resulting string to ensure valid Unicode.
     * </p>
     *
     * @param windowHandle The native handle of the GLFW window.
     * @return The clipboard content as a String, or an empty String if empty or invalid.
     */
    public String getClipboardString(long windowHandle) {
        // Temporarily replace the error callback to trap GLFW_FORMAT_UNAVAILABLE
        GLFWErrorCallback previousCallback = GLFW.glfwSetErrorCallback(null);

        GLFW.glfwSetErrorCallback((error, description) -> {
            if (error != GLFW_FORMAT_UNAVAILABLE) {
                // Delegate other errors to the previous callback or stderr
                if (previousCallback != null) {
                    previousCallback.invoke(error, description);
                } else {
                    System.err.println("GLFW Error [" + error + "]: " + MemoryUtil.memUTF8(description));
                }
            }
        });

        String result = null;
        try {
            result = GLFW.glfwGetClipboardString(windowHandle);
        } catch (Exception ignored) {
            // Catch-all for unexpected native issues, though the callback handles most.
        } finally {
            // Restore the original error callback state
            GLFW.glfwSetErrorCallback(previousCallback);
        }

        return result != null ? sanitizeString(result) : "";
    }

    /**
     * Sanitizes the input string by removing malformed Unicode surrogate pairs.
     *
     * @param input The raw string from the clipboard.
     * @return A valid Unicode string.
     */
    private String sanitizeString(String input) {
        StringBuilder sb = new StringBuilder();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);

            if (Character.isHighSurrogate(c)) {
                // Check if the next character is a valid low surrogate
                if (i + 1 < length && Character.isLowSurrogate(input.charAt(i + 1))) {
                    sb.append(c);
                    sb.append(input.charAt(i + 1));
                    i++; // Skip the next char since it was consumed
                }
                // If isolated high surrogate, it is ignored (filtered out)
            } else if (!Character.isLowSurrogate(c)) {
                // Append normal characters (that are not isolated low surrogates)
                sb.append(c);
            }
        }
        return sb.toString();
    }
}