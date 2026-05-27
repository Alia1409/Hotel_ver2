package service.ai;

import service.ReservationService;
import service.RoomService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class AiChatService {
    private final RoomService roomService;
    private final AiSettingsStore settingsStore;
    private final HotelContextBuilder contextBuilder;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "ai-chat-worker");
        t.setDaemon(true);
        return t;
    });

    private final List<ChatMessage> history = new ArrayList<>();
    private AiSettings settings;
    private boolean systemInitialized;

    public AiChatService(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.settingsStore = new AiSettingsStore();
        this.contextBuilder = new HotelContextBuilder(roomService);
        this.settings = settingsStore.load();
    }

    public AiSettings getSettings() {
        return settings;
    }

    public void reloadSettings() {
        settings = settingsStore.load();
        systemInitialized = false;
    }

    public void saveSettings(AiSettings newSettings) throws java.io.IOException {
        settingsStore.save(newSettings);
        settings = settingsStore.load();
        systemInitialized = false;
    }

    public boolean isConfigured() {
        return settings.hasApiKey();
    }

    public List<ChatMessage> getHistory() {
        return List.copyOf(history);
    }

    public void clearHistory() {
        history.clear();
        systemInitialized = false;
    }

    public void sendMessage(String userText, Consumer<ChatResult> onComplete) {
        if (userText == null || userText.isBlank()) {
            onComplete.accept(ChatResult.error("Please enter a message."));
            return;
        }
        if (!settings.hasApiKey()) {
            onComplete.accept(ChatResult.needsConfiguration());
            return;
        }

        ensureSystemMessage();
        history.add(new ChatMessage("user", userText.trim()));

        executor.submit(() -> {
            try {
                LlmClient client = LlmClientFactory.create(settings);
                List<ChatMessage> payload = new ArrayList<>(history);
                String reply = client.chat(payload);
                history.add(new ChatMessage("assistant", reply));
                onComplete.accept(ChatResult.success(reply));
            } catch (LlmException e) {
                history.remove(history.size() - 1);
                onComplete.accept(ChatResult.error(e.getMessage()));
            }
        });
    }

    private void ensureSystemMessage() {
        if (!systemInitialized) {
            history.add(0, new ChatMessage("system", contextBuilder.buildSystemPrompt()));
            systemInitialized = true;
        }
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    public static final class ChatResult {
        public enum Type { SUCCESS, ERROR, NEEDS_CONFIG }

        private final Type type;
        private final String message;

        private ChatResult(Type type, String message) {
            this.type = type;
            this.message = message;
        }

        public static ChatResult success(String reply) {
            return new ChatResult(Type.SUCCESS, reply);
        }

        public static ChatResult error(String message) {
            return new ChatResult(Type.ERROR, message);
        }

        public static ChatResult needsConfiguration() {
            return new ChatResult(Type.NEEDS_CONFIG,
                    "Configure your API key in AI Settings to start chatting.");
        }

        public Type getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}
