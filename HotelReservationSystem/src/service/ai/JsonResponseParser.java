package service.ai;

final class JsonResponseParser {
    private JsonResponseParser() {}

    static String extractAssistantContent(String json, boolean anthropic) {
        if (json == null || json.isBlank()) {
            return "";
        }
        if (anthropic) {
            return extractAnthropicText(json);
        }
        return extractOpenAiContent(json);
    }

    private static String extractOpenAiContent(String json) {
        int idx = json.indexOf("\"content\"");
        while (idx >= 0) {
            int colon = json.indexOf(':', idx);
            if (colon < 0) {
                break;
            }
            int start = skipWhitespace(json, colon + 1);
            if (start < json.length() && json.charAt(start) == '"') {
                return readJsonString(json, start);
            }
            idx = json.indexOf("\"content\"", idx + 9);
        }
        return "";
    }

    private static String extractAnthropicText(String json) {
        int idx = json.indexOf("\"text\"");
        if (idx < 0) {
            return extractOpenAiContent(json);
        }
        int colon = json.indexOf(':', idx);
        if (colon < 0) {
            return "";
        }
        int start = skipWhitespace(json, colon + 1);
        if (start < json.length() && json.charAt(start) == '"') {
            return readJsonString(json, start);
        }
        return "";
    }

    static String extractErrorMessage(String json) {
        String msg = extractField(json, "message");
        if (!msg.isBlank()) {
            return msg;
        }
        return extractField(json, "error");
    }

    private static String extractField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx < 0) {
            return "";
        }
        int colon = json.indexOf(':', idx);
        if (colon < 0) {
            return "";
        }
        int start = skipWhitespace(json, colon + 1);
        if (start < json.length() && json.charAt(start) == '"') {
            return readJsonString(json, start);
        }
        return "";
    }

    private static int skipWhitespace(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    private static String readJsonString(String json, int quoteStart) {
        if (quoteStart >= json.length() || json.charAt(quoteStart) != '"') {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = quoteStart + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"', '\\', '/' -> sb.append(next);
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (i + 5 < json.length()) {
                            String hex = json.substring(i + 2, i + 6);
                            sb.append((char) Integer.parseInt(hex, 16));
                            i += 5;
                        }
                    }
                    default -> sb.append(next);
                }
                i++;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
