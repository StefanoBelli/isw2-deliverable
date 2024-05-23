package ste.evaluation.eam.exception;

public final class NPofBxException extends Exception {
    @Override
    public String getMessage() {
        return "cannot find matching \"yes\" label";
    } 
}
