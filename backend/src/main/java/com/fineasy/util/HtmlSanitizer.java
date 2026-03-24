package com.fineasy.util;

public final class HtmlSanitizer {

    private HtmlSanitizer() {
    }

    /**
     * Escapes HTML special characters to prevent XSS attacks.
     */
    public static String escape(String input) {
        if (input == null) {
            return null;
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }
}
