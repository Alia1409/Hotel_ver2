package service.ai;

import model.Room;
import service.ReservationService;
import service.RoomService;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Headless smoke tests (run with: java --enable-preview ... or from IDE).
 * Does not call live APIs.
 */
public class AiSmokeTest {
    public static void main(String[] args) {
        testSettingsRoundTrip();
        testSystemPromptHasRoomsNoGuestPii();
        testJsonEscaper();
        try {
            testNeedsConfig();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("All smoke tests passed.");
    }

    private static void testSettingsRoundTrip() {
        AiSettingsStore store = new AiSettingsStore();
        AiSettings s = new AiSettings();
        s.setProvider(AiProvider.ANTHROPIC);
        s.setApiKey("test-key");
        s.setModel("claude-test");
        try {
            store.save(s);
            AiSettings loaded = store.load();
            assert loaded.getProvider() == AiProvider.ANTHROPIC;
            assert "test-key".equals(loaded.getApiKey());
            assert "claude-test".equals(loaded.getModel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void testSystemPromptHasRoomsNoGuestPii() {
        RoomService rs = new RoomService();
        HotelContextBuilder builder = new HotelContextBuilder(rs);
        String prompt = builder.buildSystemPrompt();
        if (!prompt.contains("Room 101") || !prompt.contains("Boutique Hotel Ver2")) {
            throw new IllegalStateException("Prompt missing room catalog");
        }
        if (prompt.contains("reservations.txt") || prompt.toLowerCase().contains("@guest")) {
            throw new IllegalStateException("Prompt must not contain reservation PII");
        }
        List<Room> rooms = rs.getAllRooms();
        for (Room r : rooms) {
            if (!prompt.contains(r.getRoomNumber())) {
                throw new IllegalStateException("Missing room " + r.getRoomNumber());
            }
        }
    }

    private static void testJsonEscaper() {
        String escaped = JsonEscaper.escape("line\n\"quote\"");
        if (!escaped.contains("\\n") || !escaped.contains("\\\"")) {
            throw new IllegalStateException("JSON escape failed");
        }
    }

    private static void testNeedsConfig() throws Exception {
        RoomService rs = new RoomService();
        ReservationService res = new ReservationService(rs);
        AiChatService chat = new AiChatService(rs, res);
        AiSettings empty = new AiSettings();
        empty.setApiKey("");
        chat.saveSettings(empty);
        chat.reloadSettings();
        if (chat.isConfigured()) {
            throw new IllegalStateException("Expected unconfigured after clearing API key");
        }
        CountDownLatch latch = new CountDownLatch(1);
        final AiChatService.ChatResult[] holder = new AiChatService.ChatResult[1];
        chat.sendMessage("hi", r -> {
            holder[0] = r;
            latch.countDown();
        });
        if (!latch.await(2, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for chat callback");
        }
        if (holder[0].getType() != AiChatService.ChatResult.Type.NEEDS_CONFIG) {
            throw new IllegalStateException("Expected NEEDS_CONFIG, got " + holder[0].getType());
        }
        chat.shutdown();
    }
}
