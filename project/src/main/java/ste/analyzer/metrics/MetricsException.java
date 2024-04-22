package ste.analyzer.metrics;

public final class MetricsException extends Exception {
    private final String message;

    public MetricsException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
