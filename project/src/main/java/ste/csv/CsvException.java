package ste.csv;

public final class CsvException extends Exception {
    @Override
    public String getMessage() {
        return "Class must have @CsvDescriptor annotation";    
    } 
}
