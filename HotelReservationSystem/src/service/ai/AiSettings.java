package service.ai;

public class AiSettings {
    private AiProvider provider = AiProvider.OPENAI;
    private String apiKey = "";
    private String model = "";
    private String baseUrl = "";

    // ADD THIS METHOD
    public static String getDefaultModel(AiProvider provider) {
        if (provider == AiProvider.ANTHROPIC) {
            return "claude-3-5-sonnet-20240620";
        }
        return "gpt-4o";
    }

    // Getters and Setters
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
    public AiProvider getProvider() { return provider; }
    public void setProvider(AiProvider provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
}
