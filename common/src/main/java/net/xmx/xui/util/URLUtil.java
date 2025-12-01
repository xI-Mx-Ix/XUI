/*
 * This file is part of XUI.
 * Licensed under MIT license.
 */
package net.xmx.xui.util;

import java.io.IOException;

/**
 * Utility class for opening URLs in the system default browser.
 * Works on Windows, macOS, and Linux without any external libraries.
 *
 * @author xI-Mx-Ix
 */
public class URLUtil {

    /**
     * Opens the specified URL in the system default browser.
     * Uses OS-specific commands similar to how Minecraft does it.
     *
     * @param url The URL to open, e.g. "https://example.com"
     */
    public static void openURL(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String[] cmd;

            if (os.contains("win")) {
                // Windows: use rundll32 to open the URL
                cmd = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
            } else if (os.contains("mac")) {
                // macOS: use 'open' command
                cmd = new String[]{"open", url};
            } else {
                // Linux / other Unix-like: use 'xdg-open'
                cmd = new String[]{"xdg-open", url};
            }

            // Launch the process and immediately close streams to avoid hanging
            Process process = Runtime.getRuntime().exec(cmd);
            process.getInputStream().close();
            process.getErrorStream().close();
            process.getOutputStream().close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}