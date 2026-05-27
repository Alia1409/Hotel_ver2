package ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import service.ReservationService;
import service.RoomService;
import service.ai.AiChatService;
import service.ai.AiProvider;
import service.ai.AiSettings;
public class AiChatView {
    private final AiChatService chatService;
    private final VBox root;
    private final VBox messagesBox;
    private final ScrollPane scrollPane;
    private final TextField inputField;
    private final Button sendButton;
    private Label thinkingLabel;

    public AiChatView(RoomService roomService, ReservationService reservationService) {
        this.chatService = new AiChatService(roomService, reservationService);
        this.root = new VBox(10);
        this.messagesBox = new VBox(8);
        this.scrollPane = new ScrollPane(messagesBox);
        this.inputField = new TextField();
        this.sendButton = new Button("Send");

        buildUi();
        showWelcomeIfNeeded();
    }

    public VBox getRoot() {
        return root;
    }

    private void buildUi() {
        root.setPadding(new Insets(15));
        root.getStyleClass().add("ai-chat-root");

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("AI Customer Support Assistant");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button settingsBtn = new Button("AI Settings");
        Button clearBtn = new Button("Clear Chat");
        settingsBtn.setOnAction(e -> openSettingsDialog());
        clearBtn.setOnAction(e -> clearChat());
        header.getChildren().addAll(title, spacer, clearBtn, settingsBtn);

        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(480);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        messagesBox.setPadding(new Insets(5));

        thinkingLabel = new Label();
        thinkingLabel.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        thinkingLabel.setVisible(false);
        thinkingLabel.setManaged(false);

        HBox inputRow = new HBox(10);
        inputField.setPromptText("Ask about check-in, rooms, rates...");
        HBox.setHgrow(inputField, Priority.ALWAYS);
        sendButton.setDefaultButton(true);
        sendButton.setOnAction(e -> sendMessage());
        inputField.setOnAction(e -> sendMessage());
        inputRow.getChildren().addAll(inputField, sendButton);

        root.getChildren().addAll(header, scrollPane, thinkingLabel, inputRow);
    }

    private void showWelcomeIfNeeded() {
        if (!chatService.isConfigured()) {
            appendMessage("assistant",
                    "Welcome! Configure your API key in AI Settings (BYOK). "
                            + "Keys are stored locally on this computer only.");
        } else {
            appendMessage("assistant",
                    "Hello! I can answer questions about Boutique Hotel Ver2 — "
                            + "check-in times, rooms, rates, and policies.");
        }
    }

    private void clearChat() {
        chatService.clearHistory();
        messagesBox.getChildren().clear();
        showWelcomeIfNeeded();
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputField.clear();
        appendMessage("user", text);
        setInputEnabled(false);
        showThinking(true);

        chatService.sendMessage(text, result -> Platform.runLater(() -> {
            showThinking(false);
            setInputEnabled(true);
            switch (result.getType()) {
                case SUCCESS -> appendMessage("assistant", result.getMessage());
                case NEEDS_CONFIG -> {
                    appendMessage("assistant", result.getMessage());
                    openSettingsDialog();
                }
                case ERROR -> appendMessage("error", result.getMessage());
            }
            scrollToBottom();
        }));
    }

    private void setInputEnabled(boolean enabled) {
        inputField.setDisable(!enabled);
        sendButton.setDisable(!enabled);
    }

    private void showThinking(boolean visible) {
        thinkingLabel.setText(visible ? "Thinking..." : "");
        thinkingLabel.setVisible(visible);
        thinkingLabel.setManaged(visible);
    }

    private void appendMessage(String role, String content) {
        HBox row = new HBox();
        row.setPadding(new Insets(4, 8, 4, 8));

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(700);
        bubble.setPadding(new Insets(10, 12, 10, 12));

        String labelText;
        if ("user".equals(role)) {
            labelText = "You";
            bubble.setStyle("-fx-background-color: #3498db; -fx-background-radius: 8;");
            row.setAlignment(Pos.CENTER_RIGHT);
        } else if ("error".equals(role)) {
            labelText = "Error";
            bubble.setStyle("-fx-background-color: #fdecea; -fx-background-radius: 8; -fx-border-color: #e74c3c;");
            row.setAlignment(Pos.CENTER_LEFT);
        } else {
            labelText = "Assistant";
            bubble.setStyle("-fx-background-color: #ecf0f1; -fx-background-radius: 8;");
            row.setAlignment(Pos.CENTER_LEFT);
        }

        Label roleLabel = new Label(labelText);
        roleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;"
                + ("user".equals(role) ? " -fx-text-fill: white;" : ""));

        Text messageText = new Text(content);
        if ("user".equals(role)) {
            messageText.setStyle("-fx-fill: white;");
        }
        TextFlow textFlow = new TextFlow(messageText);
        textFlow.setMaxWidth(680);

        bubble.getChildren().addAll(roleLabel, textFlow);
        row.getChildren().add(bubble);
        messagesBox.getChildren().add(row);
        scrollToBottom();
    }

    private void scrollToBottom() {
        Platform.runLater(() -> scrollPane.setVvalue(1.0));
    }

    private void openSettingsDialog() {
        AiSettings current = chatService.getSettings();
        Dialog<AiSettings> dialog = new Dialog<>();
        dialog.setTitle("AI Settings (BYOK)");
        dialog.setHeaderText("Bring your own API key. Stored locally in ~/.hotel-ver2/");

        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        ComboBox<AiProvider> providerBox = new ComboBox<>();
        providerBox.getItems().addAll(AiProvider.OPENAI, AiProvider.ANTHROPIC);
        providerBox.setValue(current.getProvider());

        PasswordField apiKeyField = new PasswordField();
        apiKeyField.setPromptText("sk-... or Anthropic key");
        apiKeyField.setText(current.getApiKey());

        TextField modelField = new TextField(current.getModel());
        modelField.setPromptText("Default used if empty");

        TextField baseUrlField = new TextField(current.getBaseUrl());
        baseUrlField.setPromptText("Optional — OpenAI-compatible base URL");

        Label baseUrlNote = new Label("Base URL applies to OpenAI provider only.");
        baseUrlNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        providerBox.setOnAction(e -> {
            if (modelField.getText().isBlank()) {
                modelField.setPromptText(AiSettings.getDefaultModel(providerBox.getValue()));
            }
        });

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(15));
        grid.add(new Label("Provider:"), 0, 0);
        grid.add(providerBox, 1, 0);
        grid.add(new Label("API Key:"), 0, 1);
        grid.add(apiKeyField, 1, 1);
        grid.add(new Label("Model:"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("Base URL:"), 0, 3);
        grid.add(baseUrlField, 1, 3);
        grid.add(baseUrlNote, 1, 4);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn != saveType) {
                return null;
            }
            AiSettings s = new AiSettings();
            s.setProvider(providerBox.getValue());
            s.setApiKey(apiKeyField.getText());
            s.setModel(modelField.getText());
            s.setBaseUrl(baseUrlField.getText());
            return s;
        });

        dialog.showAndWait().ifPresent(saved -> {
            try {
                chatService.saveSettings(saved);
                chatService.reloadSettings();
                new Alert(Alert.AlertType.INFORMATION, "AI settings saved.", ButtonType.OK).showAndWait();
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Could not save settings: " + ex.getMessage(), ButtonType.OK)
                        .showAndWait();
            }
        });
    }
}
