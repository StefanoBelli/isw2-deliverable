package ste.csv;

import ste.csv.annotations.CsvDescriptor;

public final class CsvWriter {
    public CsvWriter(Class<?> c) {
        c.isAnnotationPresent(CsvDescriptor.class);
    }
}
