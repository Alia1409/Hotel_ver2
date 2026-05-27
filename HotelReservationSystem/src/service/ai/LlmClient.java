package service.ai;

import java.util.List;

public interface LlmClient {
    String chat(List<ChatMessage> messages) throws LlmException;
}
