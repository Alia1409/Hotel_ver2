package service.ai;

public class LlmException extends Exception {
    private final int statusCode;

    public LlmException(String message) {
        super(message);
        this.statusCode = -1;
    }

    public LlmException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
