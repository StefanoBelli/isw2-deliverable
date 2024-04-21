package ste.analyzer;

public final class BugAnalyzerException extends Exception{
    private final String message;

    public BugAnalyzerException(String message) {
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
