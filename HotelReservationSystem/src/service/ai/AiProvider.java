package service.ai;

public enum AiProvider {
    OPENAI,
    ANTHROPIC;

    public static AiProvider fromString(String value) {
        if (value == null || value.isBlank()) {
            return OPENAI;
        }
        try {
            return AiProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OPENAI;
        }
    }
}
