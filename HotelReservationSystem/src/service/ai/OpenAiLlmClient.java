package service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

public class OpenAiLlmClient implements LlmClient {
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public OpenAiLlmClient(AiSettings settings) {
        this.apiKey = settings.getApiKey();
        this.model = settings.getModel();
        String url = settings.getBaseUrl();
        this.baseUrl = (url == null || url.isBlank())
                ? "https://api.openai.com/v1/chat/completions"
                : url.endsWith("/chat/completions") ? url : url.replaceAll("/+$", "") + "/chat/completions";
    }

    @Override
    public String chat(List<ChatMessage> messages) throws LlmException {
        String body = buildRequestBody(messages);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            if (response.statusCode() >= 400) {
                String err = JsonResponseParser.extractErrorMessage(responseBody);
                if (err.isBlank()) {
                    err = "API request failed (HTTP " + response.statusCode() + ")";
                }
                if (response.statusCode() == 401) {
                    err = "Invalid API key. Check your OpenAI key in AI Settings.";
                } else if (response.statusCode() == 429) {
                    err = "Rate limit exceeded. Please wait and try again.";
                }
                throw new LlmException(err, response.statusCode());
            }
            String content = JsonResponseParser.extractAssistantContent(responseBody, false);
            if (content.isBlank()) {
                throw new LlmException("No response content received from OpenAI.");
            }
            return content;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Could not reach OpenAI: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(JsonEscaper.escape(model)).append("\",\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage m = messages.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"role\":\"").append(JsonEscaper.escape(m.getRole()))
              .append("\",\"content\":\"").append(JsonEscaper.escape(m.getContent())).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
