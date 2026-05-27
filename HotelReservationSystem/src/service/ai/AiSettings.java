package service.ai;

public class AiSettings {
    private AiProvider provider = AiProvider.OPENAI;
    private String apiKey = "";
    private String model = "";
    private String baseUrl = "";

    public AiProvider getProvider() {
        return provider;
    }

    public void setProvider(AiProvider provider) {
        this.provider = provider != null ? provider : AiProvider.OPENAI;
    }

    public String getApiKey() {
        return apiKey != null ? apiKey : "";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey : "";
    }

    public String getModel() {
        if (model != null && !model.isBlank()) {
            return model.trim();
        }
        return getDefaultModel(provider);
    }

    public void setModel(String model) {
        this.model = model != null ? model : "";
    }

    public String getBaseUrl() {
        return baseUrl != null ? baseUrl.trim() : "";
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl : "";
    }

    public boolean hasApiKey() {
        return !getApiKey().isBlank();
    }

    public static String getDefaultModel(AiProvider provider) {
        if (provider == AiProvider.ANTHROPIC) {
            return "claude-3-5-haiku-latest";
        }
        return "gpt-4o-mini";
    }
}
