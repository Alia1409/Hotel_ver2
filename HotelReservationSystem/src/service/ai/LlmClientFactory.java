package service.ai;

public final class LlmClientFactory {
    private LlmClientFactory() {}

    public static LlmClient create(AiSettings settings) {
        if (settings.getProvider() == AiProvider.ANTHROPIC) {
            return new AnthropicLlmClient(settings);
        }
        return new OpenAiLlmClient(settings);
    }
}
