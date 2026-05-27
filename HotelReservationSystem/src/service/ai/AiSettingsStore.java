package service.ai;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AiSettingsStore {
    private static final String DIR_NAME = ".hotel-ver2";
    private static final String FILE_NAME = "ai-settings.properties";

    private final Path settingsFile;

    public AiSettingsStore() {
        String userHome = System.getProperty("user.home");
        Path dir = Path.of(userHome, DIR_NAME);
        this.settingsFile = dir.resolve(FILE_NAME);
    }

    public AiSettings load() {
        AiSettings settings = new AiSettings();
        if (!Files.exists(settingsFile)) {
            return settings;
        }
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(settingsFile)) {
            props.load(in);
            settings.setProvider(AiProvider.fromString(props.getProperty("provider", "OPENAI")));
            settings.setApiKey(props.getProperty("apiKey", ""));
            settings.setModel(props.getProperty("model", ""));
            settings.setBaseUrl(props.getProperty("baseUrl", ""));
        } catch (IOException e) {
            System.out.println("Could not load AI settings.");
        }
        return settings;
    }

    public void save(AiSettings settings) throws IOException {
        Path parent = settingsFile.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        Properties props = new Properties();
        props.setProperty("provider", settings.getProvider().name());
        props.setProperty("apiKey", settings.getApiKey());
        props.setProperty("model", settings.getModel());
        props.setProperty("baseUrl", settings.getBaseUrl());
        try (OutputStream out = Files.newOutputStream(settingsFile)) {
            props.store(out, "Hotel Ver2 AI settings (BYOK - local only)");
        }
    }
}
