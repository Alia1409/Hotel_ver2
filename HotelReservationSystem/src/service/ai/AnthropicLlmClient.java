package service.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AnthropicLlmClient implements LlmClient {
    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

    private final String apiKey;
    private final String model;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    public AnthropicLlmClient(AiSettings settings) {
        this.apiKey = settings.getApiKey();
        this.model = settings.getModel();
    }

    @Override
    public String chat(List<ChatMessage> messages) throws LlmException {
        String systemPrompt = "";
        List<ChatMessage> conversation = new ArrayList<>();
        for (ChatMessage m : messages) {
            if ("system".equals(m.getRole())) {
                systemPrompt = m.getContent();
            } else {
                conversation.add(m);
            }
        }

        String body = buildRequestBody(systemPrompt, conversation);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", API_VERSION)
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
                    err = "Invalid API key. Check your Anthropic key in AI Settings.";
                } else if (response.statusCode() == 429) {
                    err = "Rate limit exceeded. Please wait and try again.";
                }
                throw new LlmException(err, response.statusCode());
            }
            String content = JsonResponseParser.extractAssistantContent(responseBody, true);
            if (content.isBlank()) {
                throw new LlmException("No response content received from Anthropic.");
            }
            return content;
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmException("Could not reach Anthropic: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(String systemPrompt, List<ChatMessage> conversation) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(JsonEscaper.escape(model))
          .append("\",\"max_tokens\":1024");
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            sb.append(",\"system\":\"").append(JsonEscaper.escape(systemPrompt)).append("\"");
        }
        sb.append(",\"messages\":[");
        for (int i = 0; i < conversation.size(); i++) {
            ChatMessage m = conversation.get(i);
            String role = "assistant".equals(m.getRole()) ? "assistant" : "user";
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"role\":\"").append(role)
              .append("\",\"content\":\"").append(JsonEscaper.escape(m.getContent())).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
